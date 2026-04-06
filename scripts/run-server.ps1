$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Path (Join-Path $repoRoot "logs") -Force | Out-Null
& mvn -q -DskipTests package
Push-Location $repoRoot
try {
    & java -jar "$repoRoot\target\custom-minecraft-server.jar" "$repoRoot\server-settings.json"
}
finally {
    Pop-Location
}
