$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
if (!(Test-Path -LiteralPath $envFile)) { throw 'Copy .env.example to .env and set DB_PASSWORD, ADMIN_EMAIL, ADMIN_PASSWORD first.' }
foreach ($line in Get-Content -LiteralPath $envFile) {
    if ($line -match '^\s*(DB_PASSWORD|ADMIN_EMAIL|ADMIN_PASSWORD|COOKIE_SECURE)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim().Trim('"').Trim("'"), 'Process')
    }
}
if (!$env:JAVA_HOME) {
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) { $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java.Source) }
}
if (!$env:JAVA_HOME -or !(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    throw 'Set JAVA_HOME to an installed JDK 17 or 21 (not its bin directory).'
}
Push-Location (Join-Path $projectRoot 'backend')
try { & .\mvnw.cmd spring-boot:run; if ($LASTEXITCODE -ne 0) { throw 'Backend startup failed. See the Maven output above.' } }
finally { Pop-Location }
