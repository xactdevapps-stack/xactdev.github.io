Param(
    [string]$LogDir = ".\build\reports\stress",
    [string]$Serial = ""
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

function Resolve-AdbPath {
    $paths = @()
    if ($env:ANDROID_HOME) {
        $paths += Join-Path $env:ANDROID_HOME "platform-tools\\adb.exe"
    }
    if ($env:ANDROID_SDK_ROOT) {
        $paths += Join-Path $env:ANDROID_SDK_ROOT "platform-tools\\adb.exe"
    }
    if ($env:LOCALAPPDATA) {
        $paths += Join-Path $env:LOCALAPPDATA "Android\\Sdk\\platform-tools\\adb.exe"
    }

    $candidates = @($paths | Where-Object { Test-Path $_ })

    if ($candidates.Count -gt 0) {
        return $candidates[0]
    }

    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    throw "adb was not found. Install Android platform-tools or set ANDROID_HOME / ANDROID_SDK_ROOT."
}

$adbPath = Resolve-AdbPath

Write-Host "Checking for connected Android device or emulator..."
$connectedSerials = @(
    & $adbPath devices |
        Select-String "device$" |
        ForEach-Object { ($_ -split "\s+")[0].Trim() }
)

if (-not $connectedSerials -or $connectedSerials.Count -eq 0) {
    Write-Error "No connected device/emulator found. Start an emulator (or connect a device) and retry."
}

if ([string]::IsNullOrWhiteSpace($Serial)) {
    if ($connectedSerials.Count -gt 1) {
        Write-Error "Multiple devices found ($($connectedSerials -join ', ')). Re-run with -Serial <deviceSerial>."
    }
    $Serial = $connectedSerials[0]
}

if ($connectedSerials -notcontains $Serial) {
    Write-Error "Requested serial '$Serial' is not connected. Connected: $($connectedSerials -join ', ')"
}

$safeSerial = ($Serial -replace "[^a-zA-Z0-9._-]", "_")
$logPath = Join-Path $LogDir "stress-test-$safeSerial-$timestamp.log"
$historyCsvPath = Join-Path $LogDir "benchmark-history-$safeSerial.csv"
$historyJsonlPath = Join-Path $LogDir "benchmark-history-$safeSerial.jsonl"

$deviceModel = (& $adbPath -s $Serial shell getprop ro.product.model).Trim()
$deviceSdk = (& $adbPath -s $Serial shell getprop ro.build.version.sdk).Trim()
$deviceFingerprint = (& $adbPath -s $Serial shell getprop ro.build.fingerprint).Trim()

Write-Host "Target device serial: $Serial"
Write-Host "Target device model: $deviceModel (SDK $deviceSdk)"

Write-Host "Running AppStressInstrumentationTest..."
$gradleArgs = @(
    "connectedDebugAndroidTest",
    "-Pandroid.testInstrumentationRunnerArguments.class=com.evchargecalc.app.AppStressInstrumentationTest"
)
$cmd = ".\\gradlew.bat $($gradleArgs -join ' ')"
Write-Host "Command: $cmd"

$env:ANDROID_SERIAL = $Serial
& $adbPath -s $Serial logcat -c | Out-Null

& .\gradlew.bat @gradleArgs 2>&1 |
    Tee-Object -FilePath $logPath

Write-Host "Collecting STRESS_REPORT logs..."
"`n==== STRESS_REPORT (logcat) ====" | Out-File -FilePath $logPath -Append
$stressOutput = & $adbPath -s $Serial logcat -d -s STRESS_REPORT:I *:S 2>&1
$stressOutput |
    Tee-Object -FilePath $logPath -Append

$metrics = @{}
foreach ($line in $stressOutput) {
    if ($line -match "STRESS_REPORT:\s+([a-zA-Z0-9_]+)=(.+)$") {
        $metrics[$matches[1]] = $matches[2].Trim()
    }
}

$requiredMetrics = @(
    "generation_ms",
    "insert_ms",
    "read_ms",
    "mem_before_insert_mb",
    "mem_after_read_mb",
    "warnings"
)
$missing = $requiredMetrics | Where-Object { -not $metrics.ContainsKey($_) }
if ($missing.Count -gt 0) {
    Write-Error "Stress report missing expected metrics: $($missing -join ', ')"
}

if (-not (Test-Path $historyCsvPath)) {
    "run_timestamp,serial,model,sdk,generation_ms,insert_ms,read_ms,mem_before_insert_mb,mem_after_read_mb,recent_year_sessions,network_buckets,tag_buckets,total_energy_kwh,total_cost,warnings" |
        Out-File -FilePath $historyCsvPath -Encoding utf8
}

$csvRow = @(
    $timestamp,
    $Serial,
    ('"' + $deviceModel.Replace('"','""') + '"'),
    $deviceSdk,
    $metrics["generation_ms"],
    $metrics["insert_ms"],
    $metrics["read_ms"],
    $metrics["mem_before_insert_mb"],
    $metrics["mem_after_read_mb"],
    $metrics["recent_year_sessions"],
    $metrics["network_buckets"],
    $metrics["tag_buckets"],
    $metrics["total_energy_kwh"],
    $metrics["total_cost"],
    $metrics["warnings"]
) -join ","
$csvRow | Out-File -FilePath $historyCsvPath -Append -Encoding utf8

$jsonLine = [ordered]@{
    run_timestamp = $timestamp
    serial = $Serial
    model = $deviceModel
    sdk = $deviceSdk
    fingerprint = $deviceFingerprint
    generation_ms = $metrics["generation_ms"]
    insert_ms = $metrics["insert_ms"]
    read_ms = $metrics["read_ms"]
    mem_before_insert_mb = $metrics["mem_before_insert_mb"]
    mem_after_read_mb = $metrics["mem_after_read_mb"]
    recent_year_sessions = $metrics["recent_year_sessions"]
    network_buckets = $metrics["network_buckets"]
    tag_buckets = $metrics["tag_buckets"]
    total_energy_kwh = $metrics["total_energy_kwh"]
    total_cost = $metrics["total_cost"]
    warnings = $metrics["warnings"]
} | ConvertTo-Json -Compress
$jsonLine | Out-File -FilePath $historyJsonlPath -Append -Encoding utf8

$csvRows = Import-Csv $historyCsvPath
if ($csvRows.Count -ge 2) {
    $prev = $csvRows[$csvRows.Count - 2]
    $curr = $csvRows[$csvRows.Count - 1]

    function DeltaText([string]$name) {
        $prevValue = [double]($prev.$name)
        $currValue = [double]($curr.$name)
        $delta = $currValue - $prevValue
        return "{0}: {1} ({2:+0.##;-0.##;0})" -f $name, $currValue, $delta
    }

    Write-Host "Benchmark trend vs previous run:"
    Write-Host (DeltaText "generation_ms")
    Write-Host (DeltaText "insert_ms")
    Write-Host (DeltaText "read_ms")
    Write-Host (DeltaText "mem_after_read_mb")
}

Write-Host "Stress test finished."
Write-Host "Log file: $logPath"
Write-Host "Benchmark CSV: $historyCsvPath"
Write-Host "Benchmark JSONL: $historyJsonlPath"
Write-Host "Tip: Search for 'STRESS_REPORT:' lines in the log for timing, memory, and warning summaries."
