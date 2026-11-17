$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot 'backups') | Out-Null
    $file = 'fitlife-manual-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump'
    & docker compose exec -T db pg_dump -U fitlife -d fitlife -Fc -f /tmp/fitlife-manual.dump
    if ($LASTEXITCODE -ne 0) { throw 'pg_dump failed.' }
    & docker compose cp db:/tmp/fitlife-manual.dump (Join-Path $projectRoot "backups\$file")
    if ($LASTEXITCODE -ne 0) { throw 'Copying the backup failed.' }
    Write-Output "Saved backups\$file"
} finally { Pop-Location }
