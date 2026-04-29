#Requires -Version 5.0
<#
.SYNOPSIS
Builds and installs EV Charge Calc on a connected physical Android phone.

.DESCRIPTION
This script:
1. Finds adb from Android SDK
2. Optionally builds a debug or release APK
3. Detects a physical device (ignores emulators)
4. Installs the APK
5. Launches the app

.EXAMPLE
.\install-to-phone.ps1

.EXAMPLE
.\install-to-phone.ps1 -SkipBuild

.EXAMPLE
.\install-to-phone.ps1 -Serial R5CY140H8DV
#>

param(
    [ValidateSet("debug", "release")]
    [string]$BuildType = "debug",
    [switch]$SkipBuild,
    [string]$Serial
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apkPath = Join-Path $projectRoot "app\build\outputs\apk\$BuildType\app-$BuildType.apk"
$appId = "com.evchargecalc.app"
$mainActivity = ".MainActivity"

if (-not (Test-Path $adbPath)) {
    Write-Host "adb not found at: $adbPath" -ForegroundColor Red
    Write-Host "Install Android SDK Platform-Tools or update your LOCALAPPDATA Android SDK path." -ForegroundColor Yellow
    exit 1
}

Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "EV Charge Calc - Install To Phone" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan

if (-not $SkipBuild) {
    Write-Host "`n[1/4] Building $BuildType APK..." -ForegroundColor Yellow
    Set-Location $projectRoot
    & .\gradlew.bat "assemble$([char]::ToUpper($BuildType.Substring(0,1)) + $BuildType.Substring(1))"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed." -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Build successful" -ForegroundColor Green
} else {
    Write-Host "`n[1/4] Skipping build as requested" -ForegroundColor Yellow
}

if (-not (Test-Path $apkPath)) {
    Write-Host "APK not found: $apkPath" -ForegroundColor Red
    if ($SkipBuild) {
        Write-Host "Try running without -SkipBuild so the APK is generated." -ForegroundColor Yellow
    }
    exit 1
}

Write-Host "`n[2/4] Detecting connected physical device..." -ForegroundColor Yellow
& $adbPath start-server | Out-Null

$deviceLines = (& $adbPath devices) | Select-Object -Skip 1
$connectedPhysical = @()
foreach ($line in $deviceLines) {
    if ($line -match "^\s*$") { continue }
    if ($line -match "^(\S+)\s+device$") {
        $serialCandidate = $matches[1]
        if ($serialCandidate -notmatch "^emulator-") {
            $connectedPhysical += $serialCandidate
        }
    }
}

$targetSerial = $null
if ($Serial) {
    if ($connectedPhysical -contains $Serial) {
        $targetSerial = $Serial
    } else {
        Write-Host "Requested serial '$Serial' is not connected as a physical device." -ForegroundColor Red
        if ($connectedPhysical.Count -gt 0) {
            Write-Host "Connected physical devices:" -ForegroundColor Yellow
            $connectedPhysical | ForEach-Object { Write-Host "  - $_" }
        }
        exit 1
    }
} else {
    if ($connectedPhysical.Count -eq 0) {
        Write-Host "No physical Android device detected." -ForegroundColor Red
        Write-Host "Connect phone via USB and accept USB debugging prompt." -ForegroundColor Yellow
        exit 1
    }

    $targetSerial = $connectedPhysical[0]
    if ($connectedPhysical.Count -gt 1) {
        Write-Host "Multiple phones detected. Using first: $targetSerial" -ForegroundColor Yellow
        Write-Host "Use -Serial to choose a specific phone." -ForegroundColor Yellow
    }
}

Write-Host "[OK] Using device: $targetSerial" -ForegroundColor Green

Write-Host "`n[3/4] Installing APK..." -ForegroundColor Yellow
& $adbPath -s $targetSerial install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "Install failed." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Install successful" -ForegroundColor Green

Write-Host "`n[4/4] Launching app..." -ForegroundColor Yellow
& $adbPath -s $targetSerial shell am start -n "$appId/$mainActivity"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Launch failed." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] App launched" -ForegroundColor Green

Write-Host "`n===========================================" -ForegroundColor Cyan
Write-Host "[OK] Done" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Cyan
