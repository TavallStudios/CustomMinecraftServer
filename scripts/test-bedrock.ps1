$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$serverRun = $null
$pushed = $false
. "$PSScriptRoot\TestHarness.Common.ps1"
try {
  $serverRun = Start-CustomServerHarness -RepoRoot $repoRoot
  Push-Location "$repoRoot\harness"
  $pushed = $true
  npm run smoke:bedrock
  Get-Content $serverRun.StdoutLog -Tail 60
} finally {
  if ($pushed) {
    Pop-Location
  }
  if ($serverRun) {
    Stop-CustomServerHarness -ServerRun $serverRun
  }
}
