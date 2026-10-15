param(
    [string] $TestVersionRoot = 'D:\WOK步战测试\1.20.1-Forge_47.4.22'
)

$ErrorActionPreference = 'Stop'
$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$serverRoot = Join-Path $workspace 'wok_commander_support\run-production-forge'
$modsDir = Join-Path $serverRoot 'mods'
$resultPath = Join-Path $serverRoot 'jdam_production_acceptance.txt'
$forgeArgs = Join-Path $serverRoot `
        'libraries\net\minecraftforge\forge\1.20.1-47.4.22\win_args.txt'

if (-not (Test-Path -LiteralPath $forgeArgs)) {
    throw "Forge production server is not installed: $forgeArgs"
}

function Get-CurrentModuleJar([string] $ModId) {
    $propertiesPath = Join-Path $workspace "$ModId/gradle.properties"
    $versionLine = Get-Content -LiteralPath $propertiesPath | Where-Object { $_ -match '^mod_version=' }
    if (@($versionLine).Count -ne 1) { throw "Missing or ambiguous module version: $ModId" }
    $version = $versionLine.Substring('mod_version='.Length).Trim()
    return Join-Path $workspace "$ModId/build/libs/$ModId-$version.jar"
}

New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
$modSources = @(
    (Get-CurrentModuleJar 'wok_infantry'),
    (Get-CurrentModuleJar 'wok_commander_support'),
    (Join-Path $TestVersionRoot 'mods\create-1.20.1-6.0.8.jar'),
    (Join-Path $TestVersionRoot 'mods\createbigcannons-5.11.4-mc.1.20.1-forge.jar'),
    (Join-Path $TestVersionRoot 'mods\ritchiesprojectilelib-2.1.1+mc.1.20.1-forge.jar'),
    (Join-Path $TestVersionRoot 'mods\superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar'),
    (Join-Path $TestVersionRoot 'mods\geckolib-forge-1.20.1-4.8.4.jar'),
    (Join-Path $TestVersionRoot 'mods\kotlinforforge-4.12.0-all (1).jar')
)
$curios = Get-ChildItem -LiteralPath (Join-Path $TestVersionRoot 'mods') `
        -Filter '*curios-forge-5.9.1+1.20.1.jar' | Select-Object -First 1
if ($null -eq $curios) {
    throw "Missing production GameTest mod: Curios 5.9.1 for Minecraft 1.20.1"
}
$modSources += $curios.FullName
# A version bump changes the filename. Refuse an ambiguous installation instead
# of leaving an older copy of the same mod beside the current development JAR.
foreach ($modId in @('wok_infantry', 'wok_commander_support')) {
    $expectedName = [System.IO.Path]::GetFileName((Get-CurrentModuleJar $modId))
    $oldJars = @(Get-ChildItem -LiteralPath $modsDir -Filter "$modId-*.jar" |
            Where-Object { $_.Name -ne $expectedName })
    if ($oldJars.Count -gt 0) {
        throw "Move the older $modId JAR out of this isolated test server before running: $($oldJars.Name -join ', ')"
    }
}
foreach ($source in $modSources) {
    if (-not (Test-Path -LiteralPath $source)) {
        throw "Missing production GameTest mod: $source"
    }
    Copy-Item -LiteralPath $source -Destination $modsDir -Force
}

if (Test-Path -LiteralPath $resultPath) {
    Remove-Item -LiteralPath $resultPath -Force
}

$javaArgs = @(
    '-Xms512M', '-Xmx3G',
    '-Dwok.commanderSupport.runJdamAcceptance=true',
    "-Dwok.commanderSupport.jdamAcceptanceResult=$resultPath",
    '@libraries/net/minecraftforge/forge/1.20.1-47.4.22/win_args.txt',
    'nogui'
)

Push-Location $serverRoot
try {
    & 'D:\DevTools\jdk-17\bin\java.exe' @javaArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Production Forge server exited with code $LASTEXITCODE"
    }
    if (-not (Test-Path -LiteralPath $resultPath)) {
        throw "Production Forge server exited without JDAM acceptance result: $resultPath"
    }
    $result = Get-Content -Raw -LiteralPath $resultPath
    if ($result -notmatch '(?m)^status=PASS$') {
        throw "JDAM production acceptance failed:`n$result"
    }
    $result
}
finally {
    Pop-Location
}
