#Requires -Version 5.0
<#
.SYNOPSIS
Build and install EV Charge Calculator app on Android emulator

.DESCRIPTION
This script automates the following steps:
1. Build the debug APK
2. Start the emulator (if not already running)
3. Wait for emulator to be ready
4. Install the APK
5. Launch the app

.EXAMPLE
.\build-and-install.ps1
#>

param(
    [string]$AVDName = "EvCalcAVD",
    [string]$BuildType = "debug"
)

# Set up environment
$ErrorActionPreference = "Stop"
$JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
$ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:JAVA_HOME = $JAVA_HOME
$env:Path = "$JAVA_HOME\bin;$env:Path"
$env:ANDROID_SDK_ROOT = $ANDROID_SDK_ROOT

$emulatorPath = "$ANDROID_SDK_ROOT\emulator\emulator.exe"
$adbPath = "$ANDROID_SDK_ROOT\platform-tools\adb.exe"
$PROJECT_ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not (Test-Path $emulatorPath)) {
    Write-Host "Emulator executable not found at $emulatorPath" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $adbPath)) {
    Write-Host "adb executable not found at $adbPath" -ForegroundColor Red
    exit 1
}

Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "EV Charge Calculator - Build & Install" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan

# Step 1: Build APK
Write-Host "`n[1/5] Building APK..." -ForegroundColor Yellow
Set-Location $PROJECT_ROOT
& .\gradlew.bat assemble$([char]::ToUpper($BuildType.Substring(0,1)) + $BuildType.Substring(1))

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Build successful" -ForegroundColor Green

# Step 2: Check if emulator is running
Write-Host "`n[2/5] Checking emulator status..." -ForegroundColor Yellow
& $adbPath start-server | Out-Null
$devices = & $adbPath devices
$emulatorRunning = $devices | Select-String "emulator-.*\s+device"

if ($null -eq $emulatorRunning) {
    Write-Host "Starting emulator: $AVDName" -ForegroundColor Yellow
    Start-Process -FilePath $emulatorPath -ArgumentList @("-avd", $AVDName, "-no-boot-anim") | Out-Null
    Start-Sleep -Seconds 2
} else {
    Write-Host "[OK] Emulator already running" -ForegroundColor Green
}

# Step 3: Wait for emulator to be fully ready
Write-Host "`n[3/5] Waiting for emulator to be ready..." -ForegroundColor Yellow
$maxWait = 120
$waited = 0
$deviceSerial = $null

while ($waited -lt $maxWait) {
    $deviceLine = (& $adbPath devices | Select-String "^emulator-[0-9]+\s+device$" | Select-Object -First 1)
    if ($deviceLine) {
        $deviceSerial = ($deviceLine.ToString() -split "\s+")[0]
        Write-Host ("[OK] Emulator connected: {0}" -f $deviceSerial) -ForegroundColor Green
        break
    }

    Start-Sleep -Seconds 2
    $waited += 2
    Write-Host ("  Waiting for emulator connection... ({0}/{1} seconds)" -f $waited, $maxWait)
}

if (-not $deviceSerial) {
    Write-Host "No emulator detected in adb devices." -ForegroundColor Red
    exit 1
}

$waited = 0
while ($waited -lt $maxWait) {
    $bootStatus = (& $adbPath -s $deviceSerial shell getprop sys.boot_completed 2>$null | Out-String).Trim()
    if ($bootStatus -eq "1") {
        Write-Host "[OK] Emulator boot completed" -ForegroundColor Green
        break
    }

    Start-Sleep -Seconds 2
    $waited += 2
    Write-Host ("  Waiting for Android boot... ({0}/{1} seconds)" -f $waited, $maxWait)
}

if ($waited -ge $maxWait) {
    Write-Host "Emulator took too long to boot, continuing anyway..." -ForegroundColor Yellow
}

# Step 4: Install APK
Write-Host "`n[4/5] Installing APK..." -ForegroundColor Yellow
$apkPath = "$PROJECT_ROOT\app\build\outputs\apk\$BuildType\app-$BuildType.apk"

if (-not (Test-Path $apkPath)) {
    Write-Host "APK not found at $apkPath" -ForegroundColor Red
    exit 1
}

& $adbPath -s $deviceSerial install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "Installation failed!" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] APK installed" -ForegroundColor Green

# Step 5: Launch app
Write-Host "`n[5/5] Launching app..." -ForegroundColor Yellow
& $adbPath -s $deviceSerial shell am start -n com.evchargecalc.app/.MainActivity
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to launch app" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] App launched" -ForegroundColor Green

Write-Host "`n===========================================" -ForegroundColor Cyan
Write-Host "[OK] All steps completed successfully!" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Cyan
