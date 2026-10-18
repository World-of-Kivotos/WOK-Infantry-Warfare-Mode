[CmdletBinding()]
param(
    [string]$PackageRoot = $PSScriptRoot,
    [string]$DestinationRoot = (Join-Path ([Environment]::GetFolderPath('MyDocuments')) 'Wok步战附属-创伤治疗'),
    [switch]$Merge,
    [switch]$FastVerify,
    [string]$RestoreTestInstancePath,
    [string]$RestoreExternalSourcesPath,
    [string]$RestoreLegacyArmorPath,
    [string]$RestoreWokMainWorkspacePath,
    [string]$RestoreWokMainTestEnvironmentPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PackageRoot = [IO.Path]::GetFullPath($PackageRoot).TrimEnd('\')
$DestinationRoot = [IO.Path]::GetFullPath($DestinationRoot).TrimEnd('\')

function Invoke-SafeRoboCopy([string]$Source, [string]$Target) {
    New-Item -ItemType Directory -Path $Target -Force | Out-Null
    & robocopy $Source $Target /E /COPY:DAT /DCOPY:DAT /XJ /R:2 /W:1 /NP /NFL /NDL /NJH /NJS
    if ($LASTEXITCODE -ge 8) { throw "Robocopy 失败（退出码 $LASTEXITCODE）：$Source -> $Target" }
}

function Assert-EmptyOrMerge([string]$Path) {
    if (Test-Path -LiteralPath $Path) {
        $hasContent = Get-ChildItem -LiteralPath $Path -Force -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($hasContent -and -not $Merge) {
            throw "目标目录非空：$Path。请改用空目录；确实要覆盖同名文件时才加 -Merge。"
        }
    }
}

Write-Host '[1/4] 校验迁移包...'
$verify = Join-Path $PackageRoot 'Verify-WokMigration.ps1'
if (-not (Test-Path -LiteralPath $verify)) { throw "缺少校验脚本：$verify" }
& $verify -PackageRoot $PackageRoot -Fast:$FastVerify
if ($LASTEXITCODE -ne 0) { throw '迁移包校验失败，未导入。' }

$workspaceSource = Join-Path $PackageRoot 'workspace'
if (-not (Test-Path -LiteralPath (Join-Path $workspaceSource '.git') -PathType Container)) {
    throw '数据包中的工作区缺少 .git，已拒绝不完整恢复。'
}
Assert-EmptyOrMerge $DestinationRoot
Write-Host '[2/4] 恢复完整工作区...'
Invoke-SafeRoboCopy $workspaceSource $DestinationRoot

Write-Host '[3/4] 恢复可选附加数据...'
$testSource = Join-Path $PackageRoot 'extras\wok-infantry-test-instance'
if ($RestoreTestInstancePath) {
    if ($RestoreTestInstancePath -like 'D:\WOK测试\*' -or $RestoreTestInstancePath -eq 'D:\WOK测试') {
        throw '不得把 WOK步战附属测试实例恢复到 D:\WOK测试（WOK 本体专用）。'
    }
    if (-not (Test-Path -LiteralPath $testSource)) { throw '包中没有 WOK步战附属测试实例。' }
    Assert-EmptyOrMerge $RestoreTestInstancePath
    Invoke-SafeRoboCopy $testSource ([IO.Path]::GetFullPath($RestoreTestInstancePath))
} elseif (Test-Path -LiteralPath $testSource) {
    Write-Warning '包内含测试实例，但未恢复。重新运行时传 -RestoreTestInstancePath。'
}

$externalSource = Join-Path $PackageRoot 'extras\user-provided-sources'
if ($RestoreExternalSourcesPath) {
    if (-not (Test-Path -LiteralPath $externalSource)) { throw '包中没有外部素材。' }
    Assert-EmptyOrMerge $RestoreExternalSourcesPath
    Invoke-SafeRoboCopy $externalSource ([IO.Path]::GetFullPath($RestoreExternalSourcesPath))
}

$legacySource = Join-Path $PackageRoot 'extras\legacy-armor-copy-do-not-develop'
if ($RestoreLegacyArmorPath) {
    if (-not (Test-Path -LiteralPath $legacySource)) { throw '包中没有迁移前护甲副本。' }
    Assert-EmptyOrMerge $RestoreLegacyArmorPath
    Invoke-SafeRoboCopy $legacySource ([IO.Path]::GetFullPath($RestoreLegacyArmorPath))
    Write-Warning '已恢复迁移前副本；请只将工作区中的 wok_infantry_armor/ 作为权威源码。'
}

$wokMainWorkspaceSource = Join-Path $PackageRoot 'products\wok-main\workspace'
$wokMainTestSource = Join-Path $PackageRoot 'products\wok-main\test-environment'
if ($RestoreWokMainWorkspacePath) {
    if (-not (Test-Path -LiteralPath (Join-Path $wokMainWorkspaceSource '.git'))) { throw '包中没有完整的 WOK 本体 Git 工作区。' }
    if ([IO.Path]::GetFullPath($RestoreWokMainWorkspacePath).TrimEnd('\').Equals($DestinationRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'WOK 本体与 WOK步战工作区不得恢复到同一目录。'
    }
    Assert-EmptyOrMerge $RestoreWokMainWorkspacePath
    Invoke-SafeRoboCopy $wokMainWorkspaceSource ([IO.Path]::GetFullPath($RestoreWokMainWorkspacePath))
} elseif (Test-Path -LiteralPath $wokMainWorkspaceSource) {
    Write-Warning '包内含 WOK 本体工作区，但未恢复。传入 -RestoreWokMainWorkspacePath 后恢复。'
}

if ($RestoreWokMainTestEnvironmentPath) {
    if (-not (Test-Path -LiteralPath $wokMainTestSource)) { throw '包中没有 WOK 本体测试环境。' }
    if ($RestoreTestInstancePath -and
        [IO.Path]::GetFullPath($RestoreWokMainTestEnvironmentPath).TrimEnd('\').Equals([IO.Path]::GetFullPath($RestoreTestInstancePath).TrimEnd('\'), [StringComparison]::OrdinalIgnoreCase)) {
        throw 'WOK 本体与 WOK步战测试环境不得恢复到同一目录。'
    }
    Assert-EmptyOrMerge $RestoreWokMainTestEnvironmentPath
    Invoke-SafeRoboCopy $wokMainTestSource ([IO.Path]::GetFullPath($RestoreWokMainTestEnvironmentPath))
} elseif (Test-Path -LiteralPath $wokMainTestSource) {
    Write-Warning '包内含 WOK 本体测试环境，但未恢复。传入 -RestoreWokMainTestEnvironmentPath 后恢复。'
}

Write-Host '[4/4] 检查开发环境...'
Push-Location $DestinationRoot
try {
    & git -c core.longpaths=true status --short --branch
    if ($LASTEXITCODE -ne 0) { throw 'Git 无法读取恢复后的仓库。' }
    & git -c core.longpaths=true fsck --no-progress --no-dangling
    if ($LASTEXITCODE -ne 0) { throw 'Git 对象完整性检查失败。' }
    try {
        $javaVersion = (& java -version 2>&1 | Out-String).Trim()
        Write-Host $javaVersion
        if ($javaVersion -notmatch 'version "17(?:\.|\")') {
            Write-Warning '当前 Java 不是 JDK 17；请在编译 Forge 1.20.1 项目前切换到 JDK 17。'
        }
    } catch {
        Write-Warning '未找到 Java；请在新电脑安装 JDK 17。'
    }
} finally {
    Pop-Location
}

if ($RestoreWokMainWorkspacePath) {
    $mainRoot = [IO.Path]::GetFullPath($RestoreWokMainWorkspacePath).TrimEnd('\')
    Push-Location $mainRoot
    try {
        & git -c core.longpaths=true -c "safe.directory=$($mainRoot.Replace('\', '/'))" status --short --branch
        if ($LASTEXITCODE -ne 0) { throw 'Git 无法读取恢复后的 WOK 本体仓库。' }
        & git -c core.longpaths=true -c "safe.directory=$($mainRoot.Replace('\', '/'))" fsck --no-progress --no-dangling
        if ($LASTEXITCODE -ne 0) { throw 'WOK 本体 Git 对象完整性检查失败。' }
    } finally {
        Pop-Location
    }
}

Write-Host "`n恢复完成：$DestinationRoot"
Write-Host '请使用同一 ChatGPT/Codex 账号登录，并在 Codex 中将此目录添加为项目。'
Write-Host '如果测试实例的盘符改变，请同步更新 wok_vehicle_health/gradle.properties 中的 superbwarfare_dev_jar_path。'
