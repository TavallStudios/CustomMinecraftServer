$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$documentsRoot = [Environment]::GetFolderPath("MyDocuments")
$serverHome = Join-Path $documentsRoot "CustomMCServer"
$runtimeHome = Join-Path $serverHome "server"
$logsHome = Join-Path $serverHome "logs"
$distributionSource = Join-Path $repoRoot "build\\install\\custom-minecraft-server"
$settingsSource = Join-Path $repoRoot "server-settings.json"
$startScriptPath = Join-Path $serverHome "start-server.ps1"
$startCommandPath = Join-Path $serverHome "start-server.cmd"

Write-Host "Building Custom Minecraft Server distribution..."
& "$repoRoot\\gradlew.bat" installDist

if (-not (Test-Path -LiteralPath $distributionSource)) {
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

Copy-Item -LiteralPath $distributionSource -Destination $runtimeHome -Recurse
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
    & (Join-Path $runtimeHome "bin\\custom-minecraft-server.bat") $configPath
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
