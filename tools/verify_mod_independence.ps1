param(
    [Parameter(Mandatory = $true)]
    [string[]] $JarPath
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$knownProducts = @(
    'wok_trauma',
    'wok_body_health',
    'wok_infantry',
    'wok_infantry_armor',
    'wok_vehicle_health',
    'wok_armor'
)
$allowedMandatoryByProduct = @{
    'wok_infantry_armor' = @('forge', 'minecraft', 'geckolib')
    'wok_vehicle_health' = @('forge', 'minecraft', 'superbwarfare')
}
$failures = [System.Collections.Generic.List[string]]::new()
$orderingEdges = [System.Collections.Generic.List[object]]::new()

function Read-ZipText {
    param(
        [System.IO.Compression.ZipArchive] $Archive,
        [string] $EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    if ($null -eq $entry) {
        throw "Missing $EntryName"
    }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try {
        return $reader.ReadToEnd()
    }
    finally {
        $reader.Dispose()
    }
}

foreach ($requestedPath in $JarPath) {
    $resolvedPath = (Resolve-Path -LiteralPath $requestedPath).Path
    $archive = [System.IO.Compression.ZipFile]::OpenRead($resolvedPath)
    try {
        $modsToml = Read-ZipText -Archive $archive -EntryName 'META-INF/mods.toml'
        $modMatch = [regex]::Match($modsToml, '(?m)^\s*modId\s*=\s*"([a-z0-9_]+)"')
        if (-not $modMatch.Success) {
            $failures.Add("$resolvedPath has no readable modId")
            continue
        }
        $modId = $modMatch.Groups[1].Value
        $allowedMandatory = if ($allowedMandatoryByProduct.ContainsKey($modId)) {
            $allowedMandatoryByProduct[$modId]
        }
        else {
            @('forge', 'minecraft')
        }

        $dependencyBlocks = [regex]::Matches(
            $modsToml,
            '(?ms)^\[\[dependencies\.[^\]]+\]\].*?(?=^\[\[|\z)')
        foreach ($blockMatch in $dependencyBlocks) {
            $block = $blockMatch.Value
            $dependencyMatch = [regex]::Match(
                $block, '(?m)^\s*modId\s*=\s*"([a-z0-9_]+)"')
            $mandatoryMatch = [regex]::Match(
                $block, '(?m)^\s*mandatory\s*=\s*(true|false)')
            $orderingMatch = [regex]::Match(
                $block, '(?m)^\s*ordering\s*=\s*"(BEFORE|AFTER|NONE)"')
            if (-not $dependencyMatch.Success -or -not $mandatoryMatch.Success) {
                continue
            }
            $dependencyId = $dependencyMatch.Groups[1].Value
            $mandatory = $mandatoryMatch.Groups[1].Value -eq 'true'
            if ($mandatory -and $allowedMandatory -notcontains $dependencyId) {
                $failures.Add(
                    "$modId incorrectly requires optional mod '$dependencyId'")
            }
            if ($orderingMatch.Success) {
                $ordering = $orderingMatch.Groups[1].Value
                if ($ordering -eq 'AFTER') {
                    $orderingEdges.Add([pscustomobject]@{
                        Before = $dependencyId
                        After = $modId
                    })
                }
                elseif ($ordering -eq 'BEFORE') {
                    $orderingEdges.Add([pscustomobject]@{
                        Before = $modId
                        After = $dependencyId
                    })
                }
            }
        }

        $entryNames = @($archive.Entries | ForEach-Object FullName)
        foreach ($product in $knownProducts) {
            if ($product -eq $modId) {
                continue
            }
            $foreignPrefix = switch ($product) {
                'wok_trauma' { 'com/wok/trauma/' }
                'wok_body_health' { 'com/wok/bodyhealth/' }
                'wok_infantry' { 'com/wok/infantry/' }
                'wok_infantry_armor' { 'com/wok/infantryarmor/' }
                'wok_vehicle_health' { 'com/wok/vehiclehealth/' }
                'wok_armor' { 'com/wok/armor/' }
            }
            if ($entryNames.Where({ $_.StartsWith($foreignPrefix) }).Count -gt 0) {
                $failures.Add("$modId embeds classes owned by '$product'")
            }
        }

        if ($entryNames.Where({ $_.StartsWith('com/tacz/') }).Count -gt 0) {
            $failures.Add("$modId embeds TaCZ classes instead of using a soft dependency")
        }

        [pscustomobject]@{
            Mod = $modId
            Jar = [System.IO.Path]::GetFileName($resolvedPath)
            Required = $allowedMandatory -join ', '
            Result = 'PASS'
        }
    }
    finally {
        $archive.Dispose()
    }
}

for ($leftIndex = 0; $leftIndex -lt $orderingEdges.Count; $leftIndex++) {
    $left = $orderingEdges[$leftIndex]
    for ($rightIndex = $leftIndex + 1; $rightIndex -lt $orderingEdges.Count; $rightIndex++) {
        $right = $orderingEdges[$rightIndex]
        if ($left.Before -eq $right.After -and $left.After -eq $right.Before) {
            $failures.Add(
                "Optional load-order cycle: $($left.Before) <-> $($left.After)")
        }
    }
}

if ($failures.Count -gt 0) {
    foreach ($failure in $failures) {
        Write-Error $failure
    }
    exit 1
}
