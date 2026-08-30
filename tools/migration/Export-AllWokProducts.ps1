[CmdletBinding()]
param(
    [string]$Destination = (Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) 'migration-packages'),
    [switch]$SkipHashes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'Export-WokMigration.ps1') `
    -Destination $Destination `
    -Profile Full `
    -IncludeTestInstance `
    -IncludeWokMain `
    -SkipHashes:$SkipHashes

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
