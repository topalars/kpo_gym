param([Parameter(Mandatory = $true)][string]$BackupFile)
$ErrorActionPreference = 'Stop'
# This operation intentionally replaces the current database with the selected dump.
$dump = (Resolve-Path -LiteralPath $BackupFile).Path
if ([IO.Path]::GetExtension($dump) -ne '.dump') { throw 'Choose a .dump file created by pg_dump -Fc.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & docker compose stop frontend backend backup
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop application services.' }
    & docker compose cp $dump db:/tmp/fitlife-restore.dump
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy the backup.' }
    & docker compose exec -T db pg_restore -U fitlife -d fitlife --clean --if-exists --single-transaction --exit-on-error /tmp/fitlife-restore.dump
    if ($LASTEXITCODE -ne 0) { throw 'Restore failed; application services remain stopped. Check the dump and logs.' }
    & docker compose up -d backend frontend backup
    if ($LASTEXITCODE -ne 0) { throw 'Database restored but application services failed to start.' }
    Write-Output 'Database restored. Sign in again.'
} finally { Pop-Location }
