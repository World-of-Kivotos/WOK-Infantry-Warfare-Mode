param(
    [string] $ManifestPath,
    # Only check these mod IDs, e.g. -Modules wok_infantry (default: all eight modules). A
    # comma-joined string, as "powershell -File" passes it, is split as well.
    [string[]] $Modules,
    # Release gate on top of the identity checks: the current CHANGELOG entry must be finished
    # (no in-progress marker in its heading, all six sections present and none left empty) and
    # each JAR must be built after the last commit touching its module's sources or build files,
    # with no uncommitted changes there. Build again after the final commit before using it.
    [switch] $Release
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$moduleDirectories = @('.', 'wok_body_health', 'wok_infantry', 'wok_infantry_armor',
    'wok_vehicle_health', 'wok_commander_support', 'wok_capture_points', 'wok_downed')
$selectedModules = @($Modules | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } |
    Where-Object { $_ -ne '' })
# CHANGELOG markers, \u-escaped to keep this script ASCII-only: "in progress" and the six
# required sections (added, changed, fixed, compatibility, config/save impact, test results).
$inProgressMarker = [regex]::Unescape('\u8fdb\u884c\u4e2d')
$requiredSections = @(
    [regex]::Unescape('\u65b0\u589e'),
    [regex]::Unescape('\u4fee\u6539'),
    [regex]::Unescape('\u4fee\u590d'),
    [regex]::Unescape('\u517c\u5bb9\u6027'),
    [regex]::Unescape('\u914d\u7f6e/\u5b58\u6863\u5f71\u54cd'),
    [regex]::Unescape('\u6d4b\u8bd5\u7ed3\u679c'))
# Paths, relative to a module root, whose last commit a release JAR must be newer than.
$releaseSourcePaths = @('src/main', 'build.gradle', 'gradle.properties')
# Keep this script ASCII-only: Windows PowerShell 5.1 decodes a BOM-less script with the ANSI
# code page, so literal Chinese names would be garbled. CHANGELOG product names, \u-escaped:
# trauma, body health, core, standalone armor, vehicle health, commander support,
# capture points, downed.
$productNames = @{
    wok_trauma = [regex]::Unescape('WOK\u6b65\u6218\u9644\u5c5e-\u521b\u4f24\u6cbb\u7597')
    wok_body_health = [regex]::Unescape('WOK\u6b65\u6218\u9644\u5c5e-\u90e8\u4f4d\u8840\u91cf')
    wok_infantry = [regex]::Unescape('WOK\u6b65\u6218\u6838\u5fc3')
    wok_infantry_armor = [regex]::Unescape('WOK\u6b65\u6218\u9644\u5c5e-\u72ec\u7acb\u62a4\u7532')
    wok_vehicle_health = [regex]::Unescape('WOK\u6b65\u6218\u9644\u5c5e-\u8f7d\u5177\u90e8\u4f4d\u8840\u91cf')
    wok_commander_support = [regex]::Unescape('WOK\u6b65\u6218\u9644\u5c5e-\u6307\u6325\u5b98\u652f\u63f4')
    wok_capture_points = [regex]::Unescape('WOK\u6b65\u6218\u9644\u5c5e-\u5360\u70b9')
    wok_downed = [regex]::Unescape('WOK\u6b65\u6218\u9644\u5c5e-\u5012\u5730\u6551\u63f4')
}
# Explicit UTF-8: Windows PowerShell 5.1 would otherwise read BOM-less Markdown as ANSI.
$readme = Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $workspace 'README.md')
$versioning = Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $workspace 'docs/VERSIONING.md')
$changelog = Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $workspace 'CHANGELOG.md')

function Get-WorkspaceRelativePath([string] $Path) {
    # Windows PowerShell 5.1 runs on .NET Framework, which has no Path.GetRelativePath.
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $root = [System.IO.Path]::GetFullPath($workspace).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
    if (-not $fullPath.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Path is outside the workspace: $fullPath"
    }
    return $fullPath.Substring($root.Length)
}

function Read-JarText($Archive, [string] $Name) {
    $entry = $Archive.GetEntry($Name)
    if ($null -eq $entry) { throw "Missing JAR entry: $Name" }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

# Release gate, part 1: the CHANGELOG entry under $Heading (a full heading line) is finished.
function Assert-ChangelogEntryFinished([string] $Changelog, [string] $Heading, [string] $Label) {
    if ($Heading.Contains($inProgressMarker)) {
        throw "CHANGELOG entry is still marked in progress: $Label"
    }
    $lines = $Changelog -split '\r?\n'
    $start = [Array]::IndexOf($lines, $Heading)
    if ($start -lt 0) { throw "CHANGELOG entry not found: $Label" }
    $end = $lines.Length
    for ($i = $start + 1; $i -lt $lines.Length; $i++) {
        if ($lines[$i].StartsWith('## ')) { $end = $i; break }
    }
    $sections = @{}
    $current = $null
    for ($i = $start + 1; $i -lt $end; $i++) {
        if ($lines[$i].StartsWith('### ')) {
            $current = $lines[$i].Substring(4).Trim()
            $sections[$current] = 0
        } elseif ($null -ne $current -and $lines[$i].Trim() -ne '') {
            $sections[$current]++
        }
    }
    foreach ($section in $requiredSections) {
        if (-not $sections.ContainsKey($section)) {
            throw "CHANGELOG entry $Label is missing the section '### $section' (write a bullet saying none when empty)"
        }
        if ($sections[$section] -eq 0) {
            throw "CHANGELOG entry $Label has an empty section '### $section'; empty sections must say none explicitly"
        }
    }
}

# Release gate, part 2: the JAR is newer than the module's last source/build commit and the
# working tree has no uncommitted changes there.
function Assert-JarBuiltFromHead([string] $Directory, [string] $JarPath, [string] $Label) {
    $paths = @($releaseSourcePaths | ForEach-Object {
        if ($Directory -eq '.') { $_ } else { "$Directory/$_" }
    })
    $dirty = @(& git -C $workspace status --porcelain --untracked-files=normal -- @paths)
    if ($LASTEXITCODE -ne 0) { throw 'Unable to read Git status' }
    if ($dirty.Count -gt 0) {
        throw "Uncommitted source changes for ${Label}; commit them and build again"
    }
    $lastCommit = & git -C $workspace log -1 --format=%ct -- @paths
    if ($LASTEXITCODE -ne 0 -or -not $lastCommit) { throw "Unable to read the last commit for $Label" }
    $jarTime = [DateTimeOffset]::new((Get-Item -LiteralPath $JarPath).LastWriteTimeUtc).ToUnixTimeSeconds()
    if ($jarTime -lt [long] $lastCommit) {
        throw "JAR is older than the last source commit for ${Label}; build again after the final commit"
    }
}

$knownModIds = @{}
$modules = @(foreach ($directory in $moduleDirectories) {
    $moduleRoot = Join-Path $workspace $directory
    $properties = @{}
    foreach ($line in Get-Content -LiteralPath (Join-Path $moduleRoot 'gradle.properties')) {
        if ($line -match '^([a-z_]+)=(.*)$') { $properties[$Matches[1]] = $Matches[2].Trim() }
    }
    $modId = $properties['mod_id']
    $knownModIds[$modId] = $true
    if ($selectedModules.Count -gt 0 -and $selectedModules -notcontains $modId) { continue }
    $version = $properties['mod_version']
    if ($version -notmatch '^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$') {
        throw "Invalid version for ${modId}: $version"
    }
    $baseName = if ($modId -eq 'wok_infantry_armor') {
        "$modId-$($properties['mc_version'])"
    } else { $modId }
    $jarName = "$baseName-$version.jar"
    $jarPath = Join-Path $moduleRoot "build/libs/$jarName"
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "Build current version first: $jarPath"
    }
    $archive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
    try {
        $toml = Read-JarText $archive 'META-INF/mods.toml'
        $manifest = Read-JarText $archive 'META-INF/MANIFEST.MF'
        $jarModId = [regex]::Match($toml, '(?m)^\s*modId\s*=\s*"([^"]+)"').Groups[1].Value
        $jarVersion = [regex]::Match($toml, '(?m)^\s*version\s*=\s*"([^"]+)"').Groups[1].Value
        if ($jarVersion -eq '${file.jarVersion}') {
            $jarVersion = [regex]::Match($manifest, '(?m)^Implementation-Version: (.+)\r?$').Groups[1].Value.Trim()
        }
        if ($jarModId -cne $modId -or $jarVersion -cne $version) {
            throw "Version/identity mismatch: $jarName contains $jarModId $jarVersion"
        }
    } finally { $archive.Dispose() }

    foreach ($document in @($readme, $versioning)) {
        $rows = @($document -split '\r?\n' | Where-Object { $_.StartsWith('|') -and $_.Contains('`' + $modId + '`') })
        if ($rows.Count -ne 1 -or -not $rows[0].Contains('`' + $version + '`')) {
            throw "Current version table mismatch for $modId $version"
        }
    }
    $moduleReadme = Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $moduleRoot 'README.md')
    if (-not $moduleReadme.Contains($jarName)) { throw "Module README missing current JAR: $jarName" }
    $versionHeading = '(?m)^## ' + [regex]::Escape($productNames[$modId]) + ' ' + [regex]::Escape($version) + '(?: \u2014 [^\r\n]+)?\r?$'
    $headingMatch = [regex]::Match($changelog, $versionHeading)
    if (-not $headingMatch.Success) { throw "CHANGELOG missing current version: $modId $version" }
    if ($Release) {
        Assert-ChangelogEntryFinished $changelog $headingMatch.Value.TrimEnd("`r") "$modId $version"
        Assert-JarBuiltFromHead $directory $jarPath "$modId $version"
    }

    [pscustomobject]@{
        modId = $modId
        version = $version
        jar = (Get-WorkspaceRelativePath $jarPath).Replace('\', '/')
        bytes = (Get-Item -LiteralPath $jarPath).Length
        sha256 = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash.ToLowerInvariant()
    }
})
foreach ($requested in $selectedModules) {
    if (-not $knownModIds.ContainsKey($requested)) { throw "Unknown module: $requested" }
}

if ($ManifestPath) {
    $head = & git -C $workspace rev-parse HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Unable to read Git HEAD' }
    $dirty = @(& git -C $workspace status --porcelain --untracked-files=normal).Count -gt 0
    $result = [ordered]@{
        generatedAt = [DateTimeOffset]::Now.ToString('o')
        gitHead = $head
        workingTreeDirty = $dirty
        note = 'Build identity only; consult the content audit for game acceptance and release status.'
        modules = $modules
    }
    $result | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $ManifestPath -Encoding utf8
}
$modules | Format-Table modId, version, bytes, jar -AutoSize
$scope = if ($selectedModules.Count -gt 0) { ' (modules: ' + ($selectedModules -join ', ') + ')' } else { '' }
Write-Output "PASS: source versions, current docs, JAR names, mod IDs and internal versions agree$scope."
if ($Release) {
    Write-Output 'PASS (release): CHANGELOG entries finished, JARs built after the last source commit.'
}
