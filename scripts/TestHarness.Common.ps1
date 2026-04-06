function Assert-HarnessPortsAvailable {
    $tcp = Get-NetTCPConnection -LocalPort 25565 -State Listen -ErrorAction SilentlyContinue
    $udp = Get-NetUDPEndpoint -LocalPort 19132 -ErrorAction SilentlyContinue
    if ($tcp) {
        throw "TCP port 25565 is already in use by PID $($tcp.OwningProcess). Stop that process before running the harness."
    }
    if ($udp) {
        throw "UDP port 19132 is already in use by PID $($udp.OwningProcess). Stop that process before running the harness."
    }
}

function Start-CustomServerHarness {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [string]$SettingsPath = (Join-Path $RepoRoot "server-settings.json")
    )

    Assert-HarnessPortsAvailable
    & mvn -q -DskipTests package

    $logDir = Join-Path $RepoRoot "target\harness"
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    $stdoutLog = Join-Path $logDir "server-stdout.log"
    $stderrLog = Join-Path $logDir "server-stderr.log"
    Remove-Item $stdoutLog, $stderrLog -Force -ErrorAction SilentlyContinue

    $process = Start-Process `
        -FilePath "java" `
        -ArgumentList "-jar", "$RepoRoot\target\custom-minecraft-server.jar", $SettingsPath `
        -WorkingDirectory $RepoRoot `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -PassThru `
        -WindowStyle Hidden

    Wait-CustomServerReady -Process $process -StdoutLog $stdoutLog -StderrLog $stderrLog
    $javaProcessId = Get-CustomServerJavaProcessId -RepoRoot $RepoRoot

    return [pscustomobject]@{
        Process       = $process
        JavaProcessId = $javaProcessId
        StdoutLog     = $stdoutLog
        StderrLog     = $stderrLog
    }
}

function Wait-CustomServerReady {
    param(
        [Parameter(Mandatory = $true)]
        $Process,
        [Parameter(Mandatory = $true)]
        [string]$StdoutLog,
        [Parameter(Mandatory = $true)]
        [string]$StderrLog
    )

    $deadline = (Get-Date).AddSeconds(20)
    while ((Get-Date) -lt $deadline) {
        if ($Process.HasExited) {
            $stdout = if (Test-Path $StdoutLog) { Get-Content $StdoutLog -Raw } else { "" }
            $stderr = if (Test-Path $StderrLog) { Get-Content $StderrLog -Raw } else { "" }
            throw "Server exited before startup completed.`nSTDOUT:`n$stdout`nSTDERR:`n$stderr"
        }

        $stdout = if (Test-Path $StdoutLog) { Get-Content $StdoutLog -Raw } else { "" }
        if ($stdout -match "event=server_started") {
            return
        }

        Start-Sleep -Milliseconds 250
    }

    $stdoutTail = if (Test-Path $StdoutLog) { Get-Content $StdoutLog -Tail 80 | Out-String } else { "" }
    $stderrTail = if (Test-Path $StderrLog) { Get-Content $StderrLog -Tail 80 | Out-String } else { "" }
    throw "Server startup timed out.`nSTDOUT tail:`n$stdoutTail`nSTDERR tail:`n$stderrTail"
}

function Stop-CustomServerHarness {
    param(
        [Parameter(Mandatory = $true)]
        $ServerRun
    )

    if ($ServerRun.JavaProcessId) {
        Stop-Process -Id $ServerRun.JavaProcessId -Force -ErrorAction SilentlyContinue
    }
    if ($ServerRun.Process -and -not $ServerRun.Process.HasExited) {
        Stop-Process -Id $ServerRun.Process.Id -Force -ErrorAction SilentlyContinue
    }
}

function Get-CustomServerJavaProcessId {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot
    )

    $escapedRepoRoot = [regex]::Escape($RepoRoot)
    $process = Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq "java.exe" -and
            $_.CommandLine -match $escapedRepoRoot -and
            $_.CommandLine -match "custom-minecraft-server"
        } |
        Select-Object -First 1

    return $process.ProcessId
}

function New-CustomServerSettingsSnapshot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [Parameter(Mandatory = $true)]
        [ValidateSet("OFFLINE", "ONLINE")]
        [string]$AuthMode
    )

    $logDir = Join-Path $RepoRoot "target\harness"
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null

    $settings = Get-Content (Join-Path $RepoRoot "server-settings.json") -Raw | ConvertFrom-Json -AsHashtable
    $settings["authMode"] = $AuthMode

    if (-not $settings.ContainsKey("javaAuthentication")) {
        $settings["javaAuthentication"] = @{
            sessionServerUrl = "https://sessionserver.mojang.com"
            includeClientIpInSessionVerification = $false
            rsaKeySizeBits = 1024
        }
    }

    if (-not $settings.ContainsKey("bedrockAuthentication")) {
        $settings["bedrockAuthentication"] = @{
            requireTrustedRootChain = $true
            trustedRootPublicKeys = @(
                "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAECRXueJeTDqNRRgJi/vlRufByu/2G0i2Ebt6YMar5QX/R0DIIyrJMcUpruK4QveTfJSTp3Shlq4Gk34cD/4GUWwkv0DVuzeuB+tXija7HBxii03NHDbPAD0AKnLr2wdAp"
            )
        }
    }

    $snapshotPath = Join-Path $logDir "server-settings.$($AuthMode.ToLower()).json"
    $settings | ConvertTo-Json -Depth 8 | Set-Content $snapshotPath -Encoding UTF8
    return $snapshotPath
}
