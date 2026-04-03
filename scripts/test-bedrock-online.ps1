$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$serverRun = $null
$pushed = $false
. "$PSScriptRoot\TestHarness.Common.ps1"
try {
  $settingsPath = New-CustomServerSettingsSnapshot -RepoRoot $repoRoot -AuthMode ONLINE
  $serverRun = Start-CustomServerHarness -RepoRoot $repoRoot -SettingsPath $settingsPath
  Push-Location "$repoRoot\harness"
  $pushed = $true
  npm run smoke:bedrock:online
  Get-Content $serverRun.StdoutLog -Tail 80
} finally {
  if ($pushed) {
    Pop-Location
  }
  if ($serverRun) {
    Stop-CustomServerHarness -ServerRun $serverRun
  }
}
