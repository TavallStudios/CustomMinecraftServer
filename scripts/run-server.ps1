$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Path (Join-Path $repoRoot "logs") -Force | Out-Null
& "$repoRoot\gradlew.bat" installDist
Push-Location $repoRoot
try {
    & "$repoRoot\build\install\custom-minecraft-server\bin\custom-minecraft-server.bat" "$repoRoot\server-settings.json"
}
finally {
    Pop-Location
}
