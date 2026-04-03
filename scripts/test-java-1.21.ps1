$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$serverRun = $null
$pushed = $false
. "$PSScriptRoot\TestHarness.Common.ps1"
try {
  $serverRun = Start-CustomServerHarness -RepoRoot $repoRoot
  Push-Location "$repoRoot\harness"
  $pushed = $true
  npm run smoke:java:1.21
  Get-Content $serverRun.StdoutLog -Tail 40
} finally {
  if ($pushed) {
    Pop-Location
  }
  if ($serverRun) {
    Stop-CustomServerHarness -ServerRun $serverRun
  }
}
