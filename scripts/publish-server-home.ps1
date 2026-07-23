$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$documentsRoot = [Environment]::GetFolderPath("MyDocuments")
$serverHome = Join-Path $documentsRoot "CustomMCServer"
$runtimeHome = Join-Path $serverHome "server"
$logsHome = Join-Path $serverHome "logs"
$distributionSource = Join-Path $repoRoot "distribution\server"
$settingsSource = Join-Path $repoRoot "server-settings.json"
$startScriptPath = Join-Path $serverHome "start-server.ps1"
$startCommandPath = Join-Path $serverHome "start-server.cmd"

Write-Host "Building Custom Minecraft Server distribution..."
$gradle = Join-Path $repoRoot "gradlew.bat"
& $gradle --no-daemon stageDistribution
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed."
}

if (-not (Test-Path -LiteralPath $distributionSource -PathType Container)) {
    throw "Expected distribution was not created at $distributionSource"
}

New-Item -ItemType Directory -Path $serverHome -Force | Out-Null
New-Item -ItemType Directory -Path $logsHome -Force | Out-Null

if (Test-Path -LiteralPath $runtimeHome) {
    $resolvedRuntimeHome = [System.IO.Path]::GetFullPath($runtimeHome)
    $expectedRuntimeHome = [System.IO.Path]::GetFullPath((Join-Path $serverHome "server"))
    if ($resolvedRuntimeHome -ne $expectedRuntimeHome) {
        throw "Refusing to remove unexpected runtime path: $resolvedRuntimeHome"
    }
    Remove-Item -LiteralPath $runtimeHome -Recurse -Force
}

Copy-Item -LiteralPath $distributionSource -Destination $runtimeHome -Recurse -Force
Copy-Item -LiteralPath $settingsSource -Destination (Join-Path $serverHome "server-settings.json") -Force

$startScript = @'
$ErrorActionPreference = "Stop"
$serverHome = Split-Path -Parent $MyInvocation.MyCommand.Path
$runtimeHome = Join-Path $serverHome "server"
$configPath = Join-Path $serverHome "server-settings.json"
$logsHome = Join-Path $serverHome "logs"

New-Item -ItemType Directory -Path $logsHome -Force | Out-Null
Push-Location $serverHome
try {
    $classpath = "$(Join-Path $runtimeHome 'application.jar');$(Join-Path $runtimeHome 'libs\*')"
    & java -cp $classpath dev.tjxjnoobie.customminecraftserver.bootstrap.ServerMain $configPath
}
finally {
    Pop-Location
}
'@

$startCommand = @'
@echo off
powershell -NoLogo -NoExit -ExecutionPolicy Bypass -File "%~dp0start-server.ps1"
'@

Set-Content -LiteralPath $startScriptPath -Value $startScript -Encoding UTF8
Set-Content -LiteralPath $startCommandPath -Value $startCommand -Encoding ASCII

Write-Host "Published server home to $serverHome"
Write-Host "Launch it with $startScriptPath"
