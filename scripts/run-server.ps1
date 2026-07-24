$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Path (Join-Path $repoRoot "logs") -Force | Out-Null
$gradle = Join-Path $repoRoot "gradlew.bat"
& $gradle --no-daemon stageDistribution
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed."
}
Push-Location $repoRoot
try {
    & java -cp "$repoRoot\distribution\server\application.jar;$repoRoot\distribution\server\libs\*" dev.tjxjnoobie.customminecraftserver.bootstrap.ServerMain "$repoRoot\server-settings.json"
}
finally {
    Pop-Location
}
