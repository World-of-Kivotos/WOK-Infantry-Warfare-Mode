[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)]
    [int]$Port = 25566,

    [ValidateRange(30, 7200)]
    [int]$TimeoutSeconds = 600,

    [string]$GradleInitScript
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-NormalizedPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    return [System.IO.Path]::GetFullPath($Path).TrimEnd(
        [System.IO.Path]::DirectorySeparatorChar,
        [System.IO.Path]::AltDirectorySeparatorChar
    )
}

function Assert-PathWithinRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    $normalizedPath = Get-NormalizedPath -Path $Path
    $normalizedRoot = Get-NormalizedPath -Path $Root
    $rootPrefix = $normalizedRoot + [System.IO.Path]::DirectorySeparatorChar

    if (-not $normalizedPath.Equals($normalizedRoot, [System.StringComparison]::OrdinalIgnoreCase) -and
        -not $normalizedPath.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Path escapes the WOK infantry workspace: $normalizedPath"
    }

    return $normalizedPath
}

function Resolve-WorkspacePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$WorkspaceRoot
    )

    $resolvedPath = (Resolve-Path -LiteralPath $Path -ErrorAction Stop).Path
    return Assert-PathWithinRoot -Path $resolvedPath -Root $WorkspaceRoot
}

function Resolve-GradleInitScript {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$WorkspaceRoot
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw 'GradleInitScript cannot be empty when supplied.'
    }
    $candidate = if ([System.IO.Path]::IsPathRooted($Path)) {
        $Path
    }
    else {
        Join-Path $WorkspaceRoot $Path
    }
    $resolved = Resolve-WorkspacePath -Path $candidate -WorkspaceRoot $WorkspaceRoot
    $item = Get-Item -LiteralPath $resolved -Force -ErrorAction Stop
    if ($item.PSIsContainer) {
        throw "GradleInitScript must be a file: $resolved"
    }
    if (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw "GradleInitScript may not be a symbolic link or reparse point: $resolved"
    }
    if (-not ($item.Name.EndsWith('.gradle', [System.StringComparison]::OrdinalIgnoreCase) -or
            $item.Name.EndsWith('.gradle.kts', [System.StringComparison]::OrdinalIgnoreCase))) {
        throw "GradleInitScript must end in .gradle or .gradle.kts: $resolved"
    }
    return $resolved
}

function ConvertTo-CmdQuotedArgument {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    if ($Value.IndexOfAny([char[]]@('"', "`r", "`n", '%', '!', '^', '&', '|', '<', '>')) -ge 0) {
        throw "Unsafe character in command argument: $Value"
    }
    return '"' + $Value + '"'
}

function Assert-LoopbackPortAvailable {
    param(
        [Parameter(Mandatory = $true)]
        [int]$RequestedPort
    )

    $listener = $null
    try {
        $loopback = [System.Net.IPAddress]::Parse('127.0.0.1')
        $listener = [System.Net.Sockets.TcpListener]::new($loopback, $RequestedPort)
        $listener.Server.ExclusiveAddressUse = $true
        $listener.Start(1)
    }
    catch {
        throw [System.InvalidOperationException]::new(
            "127.0.0.1:$RequestedPort cannot be bound exclusively. Choose an unused loopback port.",
            $_.Exception
        )
    }
    finally {
        if ($null -ne $listener) {
            $listener.Stop()
        }
    }
}

function Read-AcceptanceSignal {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$ResultsRoot
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $null
    }

    $resolvedSignal = Resolve-WorkspacePath -Path $Path -WorkspaceRoot $ResultsRoot
    $properties = @{}
    foreach ($line in [System.IO.File]::ReadAllLines($resolvedSignal)) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#') -or $trimmed.StartsWith('!')) {
            continue
        }

        $separatorIndex = $trimmed.IndexOf('=')
        if ($separatorIndex -lt 0) {
            $separatorIndex = $trimmed.IndexOf(':')
        }
        if ($separatorIndex -lt 1) {
            throw "Malformed network-test signal: $resolvedSignal"
        }

        $key = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1).Trim()
        $properties[$key] = $value
    }

    foreach ($requiredKey in @('status', 'role', 'runId', 'detail')) {
        if (-not $properties.ContainsKey($requiredKey)) {
            throw "Signal '$resolvedSignal' is missing required property '$requiredKey'."
        }
    }

    return $properties
}

function Test-AcceptanceSignal {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$ExpectedStatus,

        [Parameter(Mandatory = $true)]
        [string]$ExpectedRole,

        [Parameter(Mandatory = $true)]
        [string]$ExpectedRunId,

        [Parameter(Mandatory = $true)]
        [string]$ResultsRoot
    )

    $signal = Read-AcceptanceSignal -Path $Path -ResultsRoot $ResultsRoot
    if ($null -eq $signal) {
        return $false
    }

    if ($signal['status'] -cne $ExpectedStatus -or
        $signal['role'] -cne $ExpectedRole -or
        $signal['runId'] -cne $ExpectedRunId) {
        throw "Unexpected signal content in '$Path': status=$($signal['status']), role=$($signal['role']), runId=$($signal['runId'])."
    }

    if ($ExpectedStatus -ceq 'PASS') {
        $expectedEvidence = [ordered]@{
            battleProtocol = '8'
            supportServiceAvailable = 'true'
            supportOptions = '0'
            supportActiveMissions = '0'
        }
        foreach ($entry in $expectedEvidence.GetEnumerator()) {
            if (-not $signal.ContainsKey($entry.Key) -or
                $signal[$entry.Key] -cne $entry.Value) {
                throw "PASS signal '$Path' has invalid $($entry.Key) evidence; expected '$($entry.Value)'."
            }
        }
    }

    return $true
}

function Assert-NoFailureSignal {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$FailureSignals,

        [Parameter(Mandatory = $true)]
        [string]$ExpectedRunId,

        [Parameter(Mandatory = $true)]
        [string]$ResultsRoot
    )

    foreach ($failureSignal in $FailureSignals) {
        $signal = Read-AcceptanceSignal -Path $failureSignal.Path -ResultsRoot $ResultsRoot
        if ($null -eq $signal) {
            continue
        }

        if ($signal['status'] -cne 'FAIL' -or
            $signal['role'] -cne $failureSignal.Role -or
            $signal['runId'] -cne $ExpectedRunId) {
            throw "Malformed or mismatched FAIL signal: $($failureSignal.Path)"
        }

        throw "Network-test role '$($failureSignal.Role)' reported FAIL: $($signal['detail'])"
    }
}

function Get-CimProcessById {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    return Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction Stop
}

function Get-CimCreationTicks {
    param(
        [Parameter(Mandatory = $true)]
        [object]$CimProcess
    )

    return ([datetime]$CimProcess.CreationDate).ToUniversalTime().Ticks
}

function Add-TrackedCimProcess {
    param(
        [Parameter(Mandatory = $true)]
        [object]$CimProcess,

        [Parameter(Mandatory = $true)]
        [hashtable]$TrackedProcesses
    )

    $processId = [int]$CimProcess.ProcessId
    $creationTicks = Get-CimCreationTicks -CimProcess $CimProcess
    if ($TrackedProcesses.ContainsKey($processId)) {
        if ([long]$TrackedProcesses[$processId] -ne $creationTicks) {
            throw "PID $processId was reused while the network test was running."
        }
        return
    }

    $TrackedProcesses[$processId] = $creationTicks
}

function Test-TrackedProcessIdentity {
    param(
        [Parameter(Mandatory = $true)]
        [object]$CimProcess,

        [Parameter(Mandatory = $true)]
        [hashtable]$TrackedProcesses
    )

    $processId = [int]$CimProcess.ProcessId
    if (-not $TrackedProcesses.ContainsKey($processId)) {
        return $false
    }

    return [long]$TrackedProcesses[$processId] -eq (Get-CimCreationTicks -CimProcess $CimProcess)
}

function Get-LiveTrackedProcessIds {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$TrackedProcesses
    )

    foreach ($trackedProcessId in @($TrackedProcesses.Keys)) {
        $currentProcess = Get-CimProcessById -ProcessId ([int]$trackedProcessId)
        if ($null -ne $currentProcess -and
            (Test-TrackedProcessIdentity -CimProcess $currentProcess -TrackedProcesses $TrackedProcesses)) {
            [int]$trackedProcessId
        }
    }
}

function Update-TrackedProcessTrees {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [int[]]$RootProcessIds,

        [Parameter(Mandatory = $true)]
        [hashtable]$TrackedProcesses
    )

    $queue = [System.Collections.Generic.Queue[int]]::new()
    foreach ($rootProcessId in $RootProcessIds) {
        $queue.Enqueue($rootProcessId)
    }
    $visited = [System.Collections.Generic.HashSet[int]]::new()

    while ($queue.Count -gt 0) {
        $parentProcessId = $queue.Dequeue()
        if (-not $visited.Add($parentProcessId)) {
            continue
        }

        $parentProcess = Get-CimProcessById -ProcessId $parentProcessId
        if ($null -eq $parentProcess -or
            -not (Test-TrackedProcessIdentity -CimProcess $parentProcess -TrackedProcesses $TrackedProcesses)) {
            continue
        }

        $children = Get-CimInstance -ClassName Win32_Process -Filter "ParentProcessId = $parentProcessId" -ErrorAction Stop
        foreach ($child in $children) {
            Add-TrackedCimProcess -CimProcess $child -TrackedProcesses $TrackedProcesses
            $queue.Enqueue([int]$child.ProcessId)
        }
    }
}

function Stop-TrackedProcessRecursive {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId,

        [Parameter(Mandatory = $true)]
        [hashtable]$TrackedProcesses,

        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.HashSet[int]]$Visited
    )

    if (-not $Visited.Add($ProcessId)) {
        return
    }

    $currentProcess = Get-CimProcessById -ProcessId $ProcessId
    if ($null -eq $currentProcess -or
        -not (Test-TrackedProcessIdentity -CimProcess $currentProcess -TrackedProcesses $TrackedProcesses)) {
        return
    }

    $children = Get-CimInstance -ClassName Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction Stop
    foreach ($child in $children) {
        Add-TrackedCimProcess -CimProcess $child -TrackedProcesses $TrackedProcesses
        Stop-TrackedProcessRecursive `
            -ProcessId ([int]$child.ProcessId) `
            -TrackedProcesses $TrackedProcesses `
            -Visited $Visited
    }

    $currentProcess = Get-CimProcessById -ProcessId $ProcessId
    if ($null -ne $currentProcess -and
        (Test-TrackedProcessIdentity -CimProcess $currentProcess -TrackedProcesses $TrackedProcesses)) {
        Stop-Process -Id $ProcessId -Force -ErrorAction Stop
    }
}

function Stop-TrackedProcessTrees {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [int[]]$RootProcessIds,

        [Parameter(Mandatory = $true)]
        [hashtable]$TrackedProcesses,

        [Parameter(Mandatory = $true)]
        [hashtable]$CapturedRootProcesses
    )

    # Bootstrap the CIM identity from the exact Process handle when startup failed
    # between Start-Process and the first normal CIM registration.
    foreach ($capturedRootProcess in @($CapturedRootProcesses.Values)) {
        try {
            $capturedRootProcess.Refresh()
            if (-not $capturedRootProcess.HasExited) {
                $cimRootProcess = Get-CimProcessById -ProcessId $capturedRootProcess.Id
                if ($null -ne $cimRootProcess) {
                    Add-TrackedCimProcess `
                        -CimProcess $cimRootProcess `
                        -TrackedProcesses $TrackedProcesses
                }
            }
        }
        catch {
            Write-Warning "Could not capture CIM identity for Gradle PID $($capturedRootProcess.Id): $($_.Exception.Message)"
        }
    }

    if ($TrackedProcesses.Count -eq 0) {
        foreach ($capturedRootProcess in @($CapturedRootProcesses.Values)) {
            try {
                $capturedRootProcess.Refresh()
                if (-not $capturedRootProcess.HasExited) {
                    Stop-Process -InputObject $capturedRootProcess -Force -ErrorAction Stop
                }
            }
            catch {
                Write-Warning "Could not stop exact captured Gradle process handle: $($_.Exception.Message)"
            }
        }
        return
    }

    try {
        Update-TrackedProcessTrees -RootProcessIds $RootProcessIds -TrackedProcesses $TrackedProcesses
    }
    catch {
        Write-Warning "Could not refresh every Gradle descendant before cleanup: $($_.Exception.Message)"
    }

    $visited = [System.Collections.Generic.HashSet[int]]::new()
    foreach ($rootProcessId in $RootProcessIds) {
        try {
            Stop-TrackedProcessRecursive `
                -ProcessId $rootProcessId `
                -TrackedProcesses $TrackedProcesses `
                -Visited $visited
        }
        catch {
            Write-Warning "Could not stop captured Gradle PID tree ${rootProcessId}: $($_.Exception.Message)"
        }
    }

    # A Gradle wrapper may have exited while its already-captured single-use daemon
    # remains alive. Visit those exact PID+creation-time identities as well.
    $visited = [System.Collections.Generic.HashSet[int]]::new()
    foreach ($trackedProcessId in @($TrackedProcesses.Keys)) {
        try {
            Stop-TrackedProcessRecursive `
                -ProcessId ([int]$trackedProcessId) `
                -TrackedProcesses $TrackedProcesses `
                -Visited $visited
        }
        catch {
            Write-Warning "Could not stop captured descendant PID ${trackedProcessId}: $($_.Exception.Message)"
        }
    }
}

function Start-NetworkGradleTask {
    param(
        [Parameter(Mandatory = $true)]
        [string]$TaskName,

        [Parameter(Mandatory = $true)]
        [string]$Role,

        [Parameter(Mandatory = $true)]
        [string]$GradleWrapper,

        [Parameter(Mandatory = $true)]
        [string]$CommandInterpreter,

        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory,

        [Parameter(Mandatory = $true)]
        [string]$RunId,

        [Parameter(Mandatory = $true)]
        [int]$RequestedPort,

        [Parameter(Mandatory = $true)]
        [string]$ResultsRoot,

        [AllowNull()]
        [string]$GradleInitScript,

        [Parameter(Mandatory = $true)]
        [hashtable]$TrackedProcesses,

        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.List[int]]$RootProcessIds,

        [Parameter(Mandatory = $true)]
        [hashtable]$CapturedRootProcesses
    )

    $stdoutCandidate = Assert-PathWithinRoot `
        -Path (Join-Path $ResultsRoot "$RunId-$Role.stdout.log") `
        -Root $ResultsRoot
    $stderrCandidate = Assert-PathWithinRoot `
        -Path (Join-Path $ResultsRoot "$RunId-$Role.stderr.log") `
        -Root $ResultsRoot

    [System.IO.File]::WriteAllText($stdoutCandidate, '', [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText($stderrCandidate, '', [System.Text.UTF8Encoding]::new($false))
    $stdoutPath = Resolve-WorkspacePath -Path $stdoutCandidate -WorkspaceRoot $ResultsRoot
    $stderrPath = Resolve-WorkspacePath -Path $stderrCandidate -WorkspaceRoot $ResultsRoot

    $gradleArguments = @()
    if ($null -ne $GradleInitScript) {
        $gradleArguments += '--init-script'
        $gradleArguments += $GradleInitScript
    }
    $gradleArguments += @(
        $TaskName,
        "-PnetworkTestRunId=$RunId",
        "-PnetworkTestPort=$RequestedPort",
        '--no-daemon',
        '--console=plain'
    )
    $quotedGradleArguments = $gradleArguments | ForEach-Object {
        ConvertTo-CmdQuotedArgument -Value $_
    }
    $batchCommand = 'call ' + (ConvertTo-CmdQuotedArgument -Value $GradleWrapper) +
        ' ' + ($quotedGradleArguments -join ' ')
    $commandArguments = @('/D', '/S', '/C', $batchCommand)

    $gradleProcess = Start-Process `
        -FilePath $CommandInterpreter `
        -ArgumentList $commandArguments `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -PassThru

    $RootProcessIds.Add([int]$gradleProcess.Id)
    $CapturedRootProcesses[[int]$gradleProcess.Id] = $gradleProcess

    $cimGradleProcess = Get-CimProcessById -ProcessId $gradleProcess.Id
    if ($null -eq $cimGradleProcess) {
        throw "Gradle task '$TaskName' exited before its PID could be captured. See $stderrPath"
    }
    Add-TrackedCimProcess -CimProcess $cimGradleProcess -TrackedProcesses $TrackedProcesses
    Update-TrackedProcessTrees -RootProcessIds @($gradleProcess.Id) -TrackedProcesses $TrackedProcesses

    return $gradleProcess
}

function Assert-ProcessHasNotFailed {
    param(
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.Process]$Process,

        [Parameter(Mandatory = $true)]
        [string]$Role,

        [Parameter(Mandatory = $true)]
        [bool]$HasPassSignal
    )

    $Process.Refresh()
    if (-not $Process.HasExited) {
        return
    }

    [void]$Process.WaitForExit()
    $Process.Refresh()
    $processExitCode = [int]$Process.ExitCode
    if ($processExitCode -ne 0) {
        throw "Gradle role '$Role' exited with code $processExitCode."
    }
    if (-not $HasPassSignal) {
        throw "Gradle role '$Role' exited cleanly before publishing its PASS signal."
    }
}

function Assert-BeforeDeadline {
    param(
        [Parameter(Mandatory = $true)]
        [datetime]$Deadline,

        [Parameter(Mandatory = $true)]
        [string]$Phase
    )

    if ([datetime]::UtcNow -ge $Deadline) {
        throw [System.TimeoutException]::new("Timed out during $Phase.")
    }
}

$exitCode = 2
$runId = [datetime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ') + '-' +
    [guid]::NewGuid().ToString('N').Substring(0, 12)
$rootProcessIds = [System.Collections.Generic.List[int]]::new()
$trackedProcesses = @{}
$capturedRootProcesses = @{}
$roleProcesses = [ordered]@{}
$resultsRoot = $null
$resolvedGradleInitScript = $null
$failureMessage = $null

try {
    $workspaceRoot = Get-NormalizedPath -Path (Resolve-Path -LiteralPath $PSScriptRoot -ErrorAction Stop).Path
    $scriptPath = Resolve-WorkspacePath -Path $PSCommandPath -WorkspaceRoot $workspaceRoot
    $gradleWrapper = Resolve-WorkspacePath `
        -Path (Join-Path $workspaceRoot 'gradlew.bat') `
        -WorkspaceRoot $workspaceRoot
    [void](Resolve-WorkspacePath -Path (Join-Path $workspaceRoot 'build.gradle') -WorkspaceRoot $workspaceRoot)
    if ($PSBoundParameters.ContainsKey('GradleInitScript')) {
        $resolvedGradleInitScript = Resolve-GradleInitScript `
            -Path $GradleInitScript `
            -WorkspaceRoot $workspaceRoot
    }
    if ([string]::IsNullOrWhiteSpace($env:ComSpec)) {
        throw 'ComSpec is not defined; cannot launch the Gradle batch wrapper reliably.'
    }
    $commandInterpreter = (Resolve-Path -LiteralPath $env:ComSpec -ErrorAction Stop).Path
    $expectedCommandInterpreter = Join-Path `
        ([Environment]::GetFolderPath([Environment+SpecialFolder]::System)) `
        'cmd.exe'
    if (-not $commandInterpreter.Equals(
            (Get-NormalizedPath -Path $expectedCommandInterpreter),
            [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "ComSpec does not resolve to the Windows system command interpreter: $commandInterpreter"
    }

    $networkTestCandidate = Assert-PathWithinRoot `
        -Path (Join-Path $workspaceRoot 'run\network-test') `
        -Root $workspaceRoot
    $resultsCandidate = Assert-PathWithinRoot `
        -Path (Join-Path $networkTestCandidate 'results') `
        -Root $workspaceRoot
    [void](New-Item -ItemType Directory -Path $resultsCandidate -Force)
    [void](Resolve-WorkspacePath -Path $networkTestCandidate -WorkspaceRoot $workspaceRoot)
    $resultsRoot = Resolve-WorkspacePath -Path $resultsCandidate -WorkspaceRoot $workspaceRoot

    Write-Host "WOK infantry core loopback network acceptance"
    Write-Host "runId=$runId"
    Write-Host "script=$scriptPath"
    Write-Host "resultsDir=$resultsRoot"
    Write-Host "gradleInitScript=$(if ($null -eq $resolvedGradleInitScript) { '<default>' } else { $resolvedGradleInitScript })"
    Write-Host "Compiling network-test source set..."

    Push-Location -LiteralPath $workspaceRoot
    try {
        $compileArguments = @()
        if ($null -ne $resolvedGradleInitScript) {
            $compileArguments += '--init-script'
            $compileArguments += $resolvedGradleInitScript
        }
        $compileArguments += @('compileNetworkTestJava', '--no-daemon', '--console=plain')
        & $gradleWrapper @compileArguments
        $compileExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
    if ($compileExitCode -ne 0) {
        throw "compileNetworkTestJava failed with exit code $compileExitCode."
    }

    Assert-LoopbackPortAvailable -RequestedPort $Port
    Write-Host "Confirmed exclusive loopback bind availability at 127.0.0.1:$Port"

    $signalPaths = @{
        HostReady = Assert-PathWithinRoot -Path (Join-Path $resultsRoot "$runId-host-ready.properties") -Root $resultsRoot
        HostPass = Assert-PathWithinRoot -Path (Join-Path $resultsRoot "$runId-host-pass.properties") -Root $resultsRoot
        HostFail = Assert-PathWithinRoot -Path (Join-Path $resultsRoot "$runId-host-fail.properties") -Root $resultsRoot
        GuestAPass = Assert-PathWithinRoot -Path (Join-Path $resultsRoot "$runId-guest-a-pass.properties") -Root $resultsRoot
        GuestAFail = Assert-PathWithinRoot -Path (Join-Path $resultsRoot "$runId-guest-a-fail.properties") -Root $resultsRoot
        GuestBPass = Assert-PathWithinRoot -Path (Join-Path $resultsRoot "$runId-guest-b-pass.properties") -Root $resultsRoot
        GuestBFail = Assert-PathWithinRoot -Path (Join-Path $resultsRoot "$runId-guest-b-fail.properties") -Root $resultsRoot
    }
    $failureSignals = @(
        [pscustomobject]@{ Role = 'host'; Path = $signalPaths.HostFail },
        [pscustomobject]@{ Role = 'guest-a'; Path = $signalPaths.GuestAFail },
        [pscustomobject]@{ Role = 'guest-b'; Path = $signalPaths.GuestBFail }
    )
    $passSignals = [ordered]@{
        'host' = $signalPaths.HostPass
        'guest-a' = $signalPaths.GuestAPass
        'guest-b' = $signalPaths.GuestBPass
    }

    $exitCode = 3
    $deadline = [datetime]::UtcNow.AddSeconds($TimeoutSeconds)
    Write-Host 'Starting loopback-only host...'
    $roleProcesses['host'] = Start-NetworkGradleTask `
        -TaskName 'runNetworkTestHost' `
        -Role 'host' `
        -GradleWrapper $gradleWrapper `
        -CommandInterpreter $commandInterpreter `
        -WorkingDirectory $workspaceRoot `
        -RunId $runId `
        -RequestedPort $Port `
        -ResultsRoot $resultsRoot `
        -GradleInitScript $resolvedGradleInitScript `
        -TrackedProcesses $trackedProcesses `
        -RootProcessIds $rootProcessIds `
        -CapturedRootProcesses $capturedRootProcesses

    while (-not (Test-AcceptanceSignal `
            -Path $signalPaths.HostReady `
            -ExpectedStatus 'READY' `
            -ExpectedRole 'host' `
            -ExpectedRunId $runId `
            -ResultsRoot $resultsRoot)) {
        Assert-NoFailureSignal `
            -FailureSignals $failureSignals `
            -ExpectedRunId $runId `
            -ResultsRoot $resultsRoot
        Assert-ProcessHasNotFailed `
            -Process $roleProcesses['host'] `
            -Role 'host' `
            -HasPassSignal $false
        Update-TrackedProcessTrees `
            -RootProcessIds $rootProcessIds.ToArray() `
            -TrackedProcesses $trackedProcesses
        Assert-BeforeDeadline -Deadline $deadline -Phase 'host startup'
        Start-Sleep -Milliseconds 250
    }

    Assert-NoFailureSignal `
        -FailureSignals $failureSignals `
        -ExpectedRunId $runId `
        -ResultsRoot $resultsRoot
    Assert-ProcessHasNotFailed `
        -Process $roleProcesses['host'] `
        -Role 'host' `
        -HasPassSignal $false
    Write-Host 'Host READY received; starting guest-a first...'
    $roleProcesses['guest-a'] = Start-NetworkGradleTask `
        -TaskName 'runNetworkTestGuestA' `
        -Role 'guest-a' `
        -GradleWrapper $gradleWrapper `
        -CommandInterpreter $commandInterpreter `
        -WorkingDirectory $workspaceRoot `
        -RunId $runId `
        -RequestedPort $Port `
        -ResultsRoot $resultsRoot `
        -GradleInitScript $resolvedGradleInitScript `
        -TrackedProcesses $trackedProcesses `
        -RootProcessIds $rootProcessIds `
        -CapturedRootProcesses $capturedRootProcesses

    $guestAPassReceived = $false
    while (-not $guestAPassReceived) {
        Assert-NoFailureSignal `
            -FailureSignals $failureSignals `
            -ExpectedRunId $runId `
            -ResultsRoot $resultsRoot
        $guestAPassReceived = Test-AcceptanceSignal `
            -Path $signalPaths.GuestAPass `
            -ExpectedStatus 'PASS' `
            -ExpectedRole 'guest-a' `
            -ExpectedRunId $runId `
            -ResultsRoot $resultsRoot
        Assert-ProcessHasNotFailed `
            -Process $roleProcesses['host'] `
            -Role 'host' `
            -HasPassSignal $false
        Assert-ProcessHasNotFailed `
            -Process $roleProcesses['guest-a'] `
            -Role 'guest-a' `
            -HasPassSignal $guestAPassReceived
        Update-TrackedProcessTrees `
            -RootProcessIds $rootProcessIds.ToArray() `
            -TrackedProcesses $trackedProcesses
        if (-not $guestAPassReceived) {
            Assert-BeforeDeadline -Deadline $deadline -Phase 'guest-a staged acceptance'
            Start-Sleep -Milliseconds 250
        }
    }

    $guestAProcess = $roleProcesses['guest-a']
    $guestAProcess.Refresh()
    if ($guestAProcess.HasExited) {
        [void]$guestAProcess.WaitForExit()
        $guestAProcess.Refresh()
        throw "guest-a exited after PASS before guest-b startup; exit code $([int]$guestAProcess.ExitCode)."
    }

    Write-Host 'guest-a PASS received and its client is still running; starting guest-b...'
    $roleProcesses['guest-b'] = Start-NetworkGradleTask `
        -TaskName 'runNetworkTestGuestB' `
        -Role 'guest-b' `
        -GradleWrapper $gradleWrapper `
        -CommandInterpreter $commandInterpreter `
        -WorkingDirectory $workspaceRoot `
        -RunId $runId `
        -RequestedPort $Port `
        -ResultsRoot $resultsRoot `
        -GradleInitScript $resolvedGradleInitScript `
        -TrackedProcesses $trackedProcesses `
        -RootProcessIds $rootProcessIds `
        -CapturedRootProcesses $capturedRootProcesses

    $allPassSignalsReceived = $false
    while (-not $allPassSignalsReceived) {
        Assert-NoFailureSignal `
            -FailureSignals $failureSignals `
            -ExpectedRunId $runId `
            -ResultsRoot $resultsRoot

        $allPassSignalsReceived = $true
        foreach ($role in $passSignals.Keys) {
            $hasPassSignal = Test-AcceptanceSignal `
                -Path $passSignals[$role] `
                -ExpectedStatus 'PASS' `
                -ExpectedRole $role `
                -ExpectedRunId $runId `
                -ResultsRoot $resultsRoot
            if (-not $hasPassSignal) {
                $allPassSignalsReceived = $false
            }
            Assert-ProcessHasNotFailed `
                -Process $roleProcesses[$role] `
                -Role $role `
                -HasPassSignal $hasPassSignal
        }

        Update-TrackedProcessTrees `
            -RootProcessIds $rootProcessIds.ToArray() `
            -TrackedProcesses $trackedProcesses
        if (-not $allPassSignalsReceived) {
            Assert-BeforeDeadline -Deadline $deadline -Phase 'network acceptance'
            Start-Sleep -Milliseconds 250
        }
    }

    Write-Host 'All three PASS signals received; waiting for natural Gradle exits...'
    $allProcessesExited = $false
    while (-not $allProcessesExited) {
        Assert-NoFailureSignal `
            -FailureSignals $failureSignals `
            -ExpectedRunId $runId `
            -ResultsRoot $resultsRoot

        Update-TrackedProcessTrees `
            -RootProcessIds $rootProcessIds.ToArray() `
            -TrackedProcesses $trackedProcesses
        $allProcessesExited = $true
        foreach ($role in $roleProcesses.Keys) {
            $networkProcess = $roleProcesses[$role]
            $networkProcess.Refresh()
            if (-not $networkProcess.HasExited) {
                $allProcessesExited = $false
                continue
            }
            [void]$networkProcess.WaitForExit()
            $networkProcess.Refresh()
            $processExitCode = [int]$networkProcess.ExitCode
            if ($processExitCode -ne 0) {
                throw "Gradle role '$role' exited with code $processExitCode after PASS."
            }
        }

        $liveTrackedProcessIds = @(Get-LiveTrackedProcessIds -TrackedProcesses $trackedProcesses)
        if ($liveTrackedProcessIds.Count -gt 0) {
            $allProcessesExited = $false
        }

        if (-not $allProcessesExited) {
            Assert-BeforeDeadline -Deadline $deadline -Phase 'natural process shutdown'
            Start-Sleep -Milliseconds 250
        }
    }

    $exitCode = 0
    Write-Host 'NETWORK TEST PASS'
}
catch {
    if ($_.Exception -is [System.TimeoutException]) {
        $exitCode = 124
    }
    $failureMessage = $_.Exception.Message
    [Console]::Error.WriteLine("NETWORK TEST FAIL: $failureMessage")
}
finally {
    Stop-TrackedProcessTrees `
        -RootProcessIds $rootProcessIds.ToArray() `
        -TrackedProcesses $trackedProcesses `
        -CapturedRootProcesses $capturedRootProcesses

    if ($null -ne $resultsRoot) {
        Write-Host "resultsDir=$resultsRoot"
    }
    else {
        Write-Host 'resultsDir=<not-created>'
    }
    Write-Host "runId=$runId"
    Write-Host "exitCode=$exitCode"
}

exit $exitCode
