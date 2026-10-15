[CmdletBinding()]
param(
    [string]$Destination = (Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) 'migration-packages'),
    [ValidateSet('Source', 'Full')]
    [string]$Profile = 'Source',
    [switch]$IncludeTestInstance,
    [switch]$IncludeExternalSources,
    [switch]$IncludeLegacyArmor,
    [switch]$IncludeWokMain,
    [string]$TestInstancePath,
    [string]$ExternalSourcePath,
    [string]$LegacyArmorPath,
    [string]$WokMainWorkspacePath,
    [string]$WokMainTestEnvironmentPath,
    [switch]$SkipHashes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$workspaceRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$workspaceRoot = [IO.Path]::GetFullPath($workspaceRoot).TrimEnd('\')
$configPath = Join-Path $PSScriptRoot 'migration.config.json'
$config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json

if (-not $TestInstancePath) { $TestInstancePath = $config.testInstancePath }
if (-not $ExternalSourcePath) { $ExternalSourcePath = $config.externalSourcePath }
if (-not $LegacyArmorPath) { $LegacyArmorPath = $config.legacyArmorPath }
if (-not $WokMainWorkspacePath) { $WokMainWorkspacePath = $config.wokMainWorkspacePath }
if (-not $WokMainTestEnvironmentPath) { $WokMainTestEnvironmentPath = $config.wokMainTestEnvironmentPath }

function Get-NormalizedPath([string]$Path) {
    return [IO.Path]::GetFullPath($Path).TrimEnd('\')
}

function Assert-NotMainProductPath([string]$Path, [string]$Purpose) {
    $candidate = Get-NormalizedPath $Path
    $forbidden = Get-NormalizedPath $config.forbiddenMainProductPath
    if ($candidate.Equals($forbidden, [StringComparison]::OrdinalIgnoreCase) -or
        $candidate.StartsWith($forbidden + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw "$Purpose 指向 WOK 本体专用目录，已拒绝：$candidate"
    }
}

function Invoke-SafeRoboCopy {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Target,
        [string[]]$ExcludeDirectories = @(),
        [string[]]$ExcludeFiles = @()
    )

    if (-not (Test-Path -LiteralPath $Source -PathType Container)) {
        throw "源目录不存在：$Source"
    }
    New-Item -ItemType Directory -Path $Target -Force | Out-Null
    $arguments = @(
        $Source, $Target, '/E', '/COPY:DAT', '/DCOPY:DAT', '/XJ',
        '/R:2', '/W:1', '/NP', '/NFL', '/NDL', '/NJH', '/NJS'
    )
    if ($ExcludeDirectories.Count -gt 0) { $arguments += '/XD'; $arguments += $ExcludeDirectories }
    if ($ExcludeFiles.Count -gt 0) { $arguments += '/XF'; $arguments += $ExcludeFiles }
    & robocopy @arguments
    $code = $LASTEXITCODE
    if ($code -ge 8) { throw "Robocopy 失败（退出码 $code）：$Source -> $Target" }
}

function Get-CommandText([string]$Name, [string[]]$Arguments) {
    try { return ((& $Name @Arguments 2>$null) -join "`n").Trim() } catch { return $null }
}

$destinationRoot = Get-NormalizedPath $Destination
if ($destinationRoot.Equals($workspaceRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw '输出目录不能等于工作区根目录。'
}
New-Item -ItemType Directory -Path $destinationRoot -Force | Out-Null

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$packagePrefix = if ($IncludeWokMain) { 'WOK-All-Products-Migration' } else { 'WOK-Infantry-Migration' }
$packageName = "$packagePrefix-$stamp-$Profile"
$finalRoot = Join-Path $destinationRoot $packageName
$stagingRoot = Join-Path $destinationRoot ('.' + $packageName + '.partial')
if ((Test-Path -LiteralPath $finalRoot) -or (Test-Path -LiteralPath $stagingRoot)) {
    throw "输出目录已存在：$finalRoot"
}
New-Item -ItemType Directory -Path $stagingRoot | Out-Null

try {
    $workspaceTarget = Join-Path $stagingRoot 'workspace'
    $excludeDirectories = @((Join-Path $workspaceRoot '.codex-temp'))
    $excludeFiles = @()

    # 包放在工作区内时必须防止自我递归复制。
    if ($destinationRoot.StartsWith($workspaceRoot + '\', [StringComparison]::OrdinalIgnoreCase)) {
        $relativeDestination = $destinationRoot.Substring($workspaceRoot.Length + 1).Split('\')[0]
        $excludeDirectories += (Join-Path $workspaceRoot $relativeDestination)
    }

    if ($Profile -eq 'Source') {
        $excludeDirectories += @(
            (Join-Path $workspaceRoot '.gradle'),
            (Join-Path $workspaceRoot 'build'),
            (Join-Path $workspaceRoot 'run'),
            (Join-Path $workspaceRoot 'tmp'),
            (Join-Path $workspaceRoot 'output'),
            (Join-Path $workspaceRoot '.tmp_sheet_optimize'),
            (Join-Path $workspaceRoot '.tmp_vehicle_validation')
        )
        foreach ($module in @('wok_body_health', 'wok_infantry', 'wok_infantry_armor', 'wok_vehicle_health')) {
            $moduleRoot = Join-Path $workspaceRoot $module
            $excludeDirectories += @(
                (Join-Path $moduleRoot '.gradle'),
                (Join-Path $moduleRoot 'build'),
                (Join-Path $moduleRoot 'run'),
                (Join-Path $moduleRoot 'tmp'),
                (Join-Path $moduleRoot 'output')
            )
        }
        $infantryRoot = Join-Path $workspaceRoot 'wok_infantry'
        if (Test-Path -LiteralPath $infantryRoot) {
            $excludeDirectories += @(Get-ChildItem -LiteralPath $infantryRoot -Directory -Filter 'build-*' -ErrorAction SilentlyContinue | ForEach-Object FullName)
        }
        $excludeFiles = @('*.log', '*.log.gz', 'javac.*.args')
    }

    Write-Host "[1/5] 复制工作区（$Profile）..."
    Invoke-SafeRoboCopy -Source $workspaceRoot -Target $workspaceTarget -ExcludeDirectories ($excludeDirectories | Select-Object -Unique) -ExcludeFiles $excludeFiles

    $extras = [ordered]@{}
    if ($IncludeTestInstance) {
        Assert-NotMainProductPath $TestInstancePath 'WOK步战附属测试实例'
        Write-Host '[2/5] 复制 WOK步战附属测试实例...'
        $target = Join-Path $stagingRoot 'extras\wok-infantry-test-instance'
        Invoke-SafeRoboCopy -Source $TestInstancePath -Target $target -ExcludeDirectories @() -ExcludeFiles @('*.log', '*.log.gz')
        $extras.testInstance = [ordered]@{ source = (Get-NormalizedPath $TestInstancePath); packagePath = 'extras/wok-infantry-test-instance' }
    }
    if ($IncludeExternalSources) {
        Write-Host '[2/5] 复制用户提供的外部素材...'
        $target = Join-Path $stagingRoot 'extras\user-provided-sources'
        Invoke-SafeRoboCopy -Source $ExternalSourcePath -Target $target
        $extras.externalSources = [ordered]@{ source = (Get-NormalizedPath $ExternalSourcePath); packagePath = 'extras/user-provided-sources' }
    }
    if ($IncludeLegacyArmor) {
        Write-Warning '正在归档迁移前独立护甲副本；恢复后不得将它当作 Git 权威源码。'
        $target = Join-Path $stagingRoot 'extras\legacy-armor-copy-do-not-develop'
        Invoke-SafeRoboCopy -Source $LegacyArmorPath -Target $target -ExcludeDirectories @((Join-Path $LegacyArmorPath '.gradle'), (Join-Path $LegacyArmorPath 'build'), (Join-Path $LegacyArmorPath 'run'))
        $extras.legacyArmor = [ordered]@{ source = (Get-NormalizedPath $LegacyArmorPath); packagePath = 'extras/legacy-armor-copy-do-not-develop' }
    }

    if ($IncludeWokMain) {
        $mainWorkspace = Get-NormalizedPath $WokMainWorkspacePath
        $mainTestEnvironment = Get-NormalizedPath $WokMainTestEnvironmentPath
        if (-not (Test-Path -LiteralPath (Join-Path $mainWorkspace '.git'))) {
            throw "WOK 本体源码目录缺少 .git：$mainWorkspace"
        }
        if ($mainWorkspace.Equals($workspaceRoot, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'WOK 本体源码与 WOK步战工作区不得指向同一目录。'
        }
        if ($mainTestEnvironment.Equals((Get-NormalizedPath $TestInstancePath), [StringComparison]::OrdinalIgnoreCase)) {
            throw 'WOK 本体与 WOK步战测试环境不得指向同一目录。'
        }

        Write-Host '[2/5] 复制 WOK 本体 Git 工作区...'
        $mainWorkspaceTarget = Join-Path $stagingRoot 'products\wok-main\workspace'
        $mainExclusions = @()
        $mainExcludeFiles = @()
        if ($Profile -eq 'Source') {
            $mainExclusions = @(
                (Join-Path $mainWorkspace '.gradle'),
                (Join-Path $mainWorkspace '.pnpm-store'),
                (Join-Path $mainWorkspace 'build'),
                (Join-Path $mainWorkspace 'run'),
                (Join-Path $mainWorkspace 'tmp')
            )
            $mainExcludeFiles = @('*.log', '*.log.gz')
        }
        Invoke-SafeRoboCopy -Source $mainWorkspace -Target $mainWorkspaceTarget -ExcludeDirectories $mainExclusions -ExcludeFiles $mainExcludeFiles

        Write-Host '[2/5] 复制 WOK 本体专用测试环境...'
        $mainTestTarget = Join-Path $stagingRoot 'products\wok-main\test-environment'
        Invoke-SafeRoboCopy -Source $mainTestEnvironment -Target $mainTestTarget
        $extras.wokMain = [ordered]@{
            workspaceSource = $mainWorkspace
            workspacePackagePath = 'products/wok-main/workspace'
            testEnvironmentSource = $mainTestEnvironment
            testEnvironmentPackagePath = 'products/wok-main/test-environment'
            git = [ordered]@{
                branch = Get-CommandText 'git' @('-c', "safe.directory=$($mainWorkspace.Replace('\', '/'))", '-C', $mainWorkspace, 'branch', '--show-current')
                head = Get-CommandText 'git' @('-c', "safe.directory=$($mainWorkspace.Replace('\', '/'))", '-C', $mainWorkspace, 'rev-parse', 'HEAD')
                remote = Get-CommandText 'git' @('-c', "safe.directory=$($mainWorkspace.Replace('\', '/'))", '-C', $mainWorkspace, 'remote', 'get-url', 'origin')
                status = Get-CommandText 'git' @('-c', "safe.directory=$($mainWorkspace.Replace('\', '/'))", '-C', $mainWorkspace, 'status', '--short', '--branch')
            }
        }
    }

    Write-Host '[3/5] 写入迁移上下文...'
    foreach ($file in @('Export-AllWokProducts.ps1', 'Import-WokMigration.ps1', 'Verify-WokMigration.ps1', 'README.md', 'migration.config.json')) {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination (Join-Path $stagingRoot $file)
    }

    $gitStatus = Get-CommandText 'git' @('-C', $workspaceRoot, 'status', '--short', '--branch')
    $packageWarnings = [Collections.Generic.List[string]]::new()
    $packageWarnings.Add('凭据、SSH/GPG 私钥、浏览器会话与 Codex 登录信息未打包。')
    $packageWarnings.Add('第三方 JAR/整合包仅供用户自己的设备迁移，不得公开分发。')
    if (-not $IncludeWokMain) { $packageWarnings.Add('D:\\WOK测试 中的 WOK 本体数据未并入。') }

    $metadata = [ordered]@{
        schemaVersion = 1
        packageName = $packageName
        createdAt = (Get-Date).ToUniversalTime().ToString('o')
        sourceComputer = $env:COMPUTERNAME
        sourceUser = $env:USERNAME
        sourceWorkspace = $workspaceRoot
        profile = $Profile
        productLine = if ($IncludeWokMain) { 'WOK + WOK步战（分离存储）' } else { 'WOK步战' }
        workspaceName = $config.workspaceName
        git = [ordered]@{
            branch = Get-CommandText 'git' @('-C', $workspaceRoot, 'branch', '--show-current')
            head = Get-CommandText 'git' @('-C', $workspaceRoot, 'rev-parse', 'HEAD')
            remote = Get-CommandText 'git' @('-C', $workspaceRoot, 'remote', 'get-url', 'origin')
            status = $gitStatus
        }
        tools = [ordered]@{
            powershell = $PSVersionTable.PSVersion.ToString()
            git = Get-CommandText 'git' @('--version')
            java = Get-CommandText 'java' @('-version')
        }
        extras = $extras
        exclusions = @($excludeDirectories | ForEach-Object { $_.Substring($workspaceRoot.Length).TrimStart('\') })
        warnings = @($packageWarnings)
    }
    $metadata | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $stagingRoot 'migration-metadata.json') -Encoding utf8

    Write-Host '[4/5] 生成文件清单与 SHA-256...'
    $manifestPath = Join-Path $stagingRoot 'manifest.sha256.csv'
    $files = @(Get-ChildItem -LiteralPath $stagingRoot -File -Recurse -Force | Where-Object FullName -ne $manifestPath)
    $manifestRows = foreach ($file in $files) {
        $relative = $file.FullName.Substring($stagingRoot.Length + 1).Replace('\', '/')
        $hash = if ($SkipHashes) { '' } else { (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash }
        [pscustomobject]@{
            RelativePath = $relative
            Length = $file.Length
            SHA256 = $hash
            LastWriteUtc = $file.LastWriteTimeUtc.ToString('o')
        }
    }
    $manifestRows | Export-Csv -LiteralPath $manifestPath -NoTypeInformation -Encoding utf8

    $marker = [ordered]@{
        complete = $true
        packageName = $packageName
        fileCount = $files.Count
        payloadBytes = [long](($files | Measure-Object Length -Sum).Sum)
        hashesIncluded = -not $SkipHashes
        completedAt = (Get-Date).ToUniversalTime().ToString('o')
    }
    $marker | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $stagingRoot '.wok-migration-package.json') -Encoding utf8

    Write-Host '[5/5] 完成数据包...'
    Move-Item -LiteralPath $stagingRoot -Destination $finalRoot
    Write-Host "`n迁移包已生成：$finalRoot"
    Write-Host ("有效载荷：{0:N2} GiB，文件：{1}" -f ($marker.payloadBytes / 1GB), $marker.fileCount)
    Write-Host "新电脑上运行：.\Import-WokMigration.ps1 -DestinationRoot '<新工作区路径>'"
}
catch {
    Write-Error $_
    Write-Warning "未完成的临时目录已保留供排查：$stagingRoot"
    exit 1
}
