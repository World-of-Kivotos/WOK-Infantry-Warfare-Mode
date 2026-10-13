param(
    [string]$TestRoot = 'D:\WOK步战测试\1.20.1-Forge_47.4.22',
    [string]$WorldName = '新的世界 (1)'
)

$ErrorActionPreference = 'Stop'
$utf8 = [System.Text.UTF8Encoding]::new($false)
$loadoutPath = Join-Path $TestRoot 'config\wok_infantry\loadouts.json'
$bodyHealthPath = Join-Path $TestRoot 'config\wok_body_health-common.toml'
$armorPath = Join-Path $TestRoot "saves\$WorldName\serverconfig\wok-infantry-armor.toml"
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'

foreach ($path in @($loadoutPath, $bodyHealthPath, $armorPath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required balance file is missing: $path"
    }
    Copy-Item -LiteralPath $path -Destination "$path.balance-$timestamp.bak"
}

function Get-ClassById {
    param([object]$Config, [string]$ClassId)
    $matches = @($Config.classes | Where-Object { $_.id -eq $ClassId })
    if ($matches.Count -ne 1) {
        throw "Expected exactly one loadout class '$ClassId', found $($matches.Count)."
    }
    return $matches[0]
}

function Get-EntryById {
    param([object]$Class, [string]$EntryId)
    $matches = @()
    foreach ($slot in $Class.slots.PSObject.Properties) {
        $matches += @($slot.Value | Where-Object { $_.id -eq $EntryId })
    }
    if ($matches.Count -ne 1) {
        throw "Expected exactly one '$EntryId' entry in '$($Class.displayName)', found $($matches.Count)."
    }
    return $matches[0]
}

function Set-FloatTag {
    param([string]$Snbt, [string]$Key, [string]$Value)
    $pattern = ",?$([regex]::Escape($Key)):[-+0-9.Ee]+f"
    $updated = [regex]::Replace($Snbt, $pattern, '')
    if ($Value -eq '1.0') {
        return $updated
    }
    if (-not $updated.EndsWith('}')) {
        throw "Invalid gun SNBT while setting $Key"
    }
    return $updated.Substring(0, $updated.Length - 1) + ",$Key`:$Value" + 'f}'
}

function Set-Handling {
    param(
        [object]$Entry,
        [string]$Ads,
        [string]$Vertical,
        [string]$Horizontal,
        [string]$Spread = '1.0'
    )
    $entry.snbt = Set-FloatTag $entry.snbt 'wok_infantry_ads_speed_scale' $Ads
    $entry.snbt = Set-FloatTag $entry.snbt 'wok_infantry_vertical_recoil_scale' $Vertical
    $entry.snbt = Set-FloatTag $entry.snbt 'wok_infantry_horizontal_recoil_scale' $Horizontal
    $entry.snbt = Set-FloatTag $entry.snbt 'wok_infantry_spread_scale' $Spread
}

$config = Get-Content -Raw -LiteralPath $loadoutPath | ConvertFrom-Json
$leader = Get-ClassById $config 'custom_8602eece5ba1'
$rifleman = Get-ClassById $config 'copy_95873c56176f4c5f'
$recon = Get-ClassById $config 'custom_ac0f9e6f4cfd'
$machineGunner = Get-ClassById $config 'copy_7363b16aef164daa'

foreach ($class in @($leader, $rifleman)) {
    Set-Handling (Get-EntryById $class 'm4a1') '0.60' '3.75' '3.00'
    $m4Acog = Get-EntryById $class 'm4a1_2'
    Set-Handling $m4Acog '0.45' '4.25' '3.50'
    $m4Acog.displayName = 'M4A1 卡宾枪 ACOG'
}

foreach ($class in @($leader, $rifleman, $recon, $machineGunner)) {
    Set-Handling (Get-EntryById $class 'm9a4') '0.75' '2.25' '1.75'
}

$m249 = Get-EntryById $machineGunner 'm249'
Set-Handling $m249 '0.30' '3.50' '3.00'
$m249.snbt = $m249.snbt.Replace('rfp:scope_acog_ta648', 'suffuse:scope_acogta01')
$m249.snbt = $m249.snbt.Replace(',ZoomNumber:2', '')
$m249.snbt = Set-FloatTag $m249.snbt 'wok_infantry_armor_ignore_scale' '1.1666667'
$m249.snbt = Set-FloatTag $m249.snbt 'wok_infantry_rpm_scale' '0.9375'
$m249.displayName = 'M249 机枪+ACOG 4倍镜+两脚架'

$m24 = Get-EntryById $recon 'm24_renewed'
$m24.snbt = Set-FloatTag $m24.snbt 'wok_infantry_damage_scale' '0.7272727'
$m24.snbt = Set-FloatTag $m24.snbt 'wok_infantry_armor_ignore_scale' '0.8333333'

# Serialization deliberately leaves count and ammoReserveLimit untouched.
$json = $config | ConvertTo-Json -Depth 100
[System.IO.File]::WriteAllText($loadoutPath, $json + [Environment]::NewLine, $utf8)

$bodyHealth = Get-Content -Raw -LiteralPath $bodyHealthPath
$bodyHealth = [regex]::Replace(
    $bodyHealth,
    '(?m)^(\s*bodyDamageScale\s*=\s*)[-+0-9.Ee]+\s*$',
    '${1}8.0')
[System.IO.File]::WriteAllText($bodyHealthPath, $bodyHealth, $utf8)

$armor = Get-Content -Raw -LiteralPath $armorPath
$lightPattern = [regex]::new('(?m)^(\s*light\s*=\s*)[-+0-9.Ee]+\s*$')
$mediumPattern = [regex]::new('(?m)^(\s*medium\s*=\s*)[-+0-9.Ee]+\s*$')
$armor = $lightPattern.Replace($armor, '${1}0.0', 1)
$armor = $mediumPattern.Replace($armor, '${1}-0.05', 1)
[System.IO.File]::WriteAllText($armorPath, $armor, $utf8)

Write-Output "Balanced loadout: $loadoutPath"
Write-Output "Balanced body health: $bodyHealthPath"
Write-Output "Balanced armor movement: $armorPath"
Write-Output 'Ammo reserve limits, ammunition crates, and throwable counts were not changed.'
