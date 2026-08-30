[CmdletBinding()]
param(
    [string]$PackageRoot = $PSScriptRoot,
    [switch]$Fast
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PackageRoot = [IO.Path]::GetFullPath($PackageRoot).TrimEnd('\')
$markerPath = Join-Path $PackageRoot '.wok-migration-package.json'
$manifestPath = Join-Path $PackageRoot 'manifest.sha256.csv'

if (-not (Test-Path -LiteralPath $markerPath -PathType Leaf)) { throw "这不是完整的 WOK 迁移包：缺少 $markerPath" }
if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) { throw "缺少校验清单：$manifestPath" }

$marker = Get-Content -LiteralPath $markerPath -Raw | ConvertFrom-Json
if (-not $marker.complete) { throw '迁移包没有完成标记。' }
$rows = @(Import-Csv -LiteralPath $manifestPath)
$errors = [Collections.Generic.List[string]]::new()
$checked = 0

foreach ($row in $rows) {
    $path = Join-Path $PackageRoot ($row.RelativePath.Replace('/', '\'))
    $file = [IO.FileInfo]::new($path)
    if (-not $file.Exists) {
        $errors.Add("缺少：$($row.RelativePath)")
        continue
    }
    if ([long]$row.Length -ne $file.Length) {
        $errors.Add("大小不符：$($row.RelativePath)")
        continue
    }
    if (-not $Fast -and $row.SHA256) {
        $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
        if ($actual -ne $row.SHA256) { $errors.Add("哈希不符：$($row.RelativePath)") }
    }
    $checked++
}

if ($rows.Count -ne [int]$marker.fileCount) {
    $errors.Add("清单文件数 $($rows.Count) 与完成标记 $($marker.fileCount) 不一致。")
}
if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    throw "校验失败，共 $($errors.Count) 项错误。"
}

Write-Host ("校验通过：{0}，{1} 个文件，{2:N2} GiB。" -f $marker.packageName, $checked, ([long]$marker.payloadBytes / 1GB))
exit 0
