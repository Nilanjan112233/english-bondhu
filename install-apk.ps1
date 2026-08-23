param(
    [string]$ApkPath = "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
)

$ErrorActionPreference = "Stop"
$adbCandidates = @(
    (Join-Path $PSScriptRoot ".tools\android-sdk\platform-tools\adb.exe")
    if ($env:ANDROID_SDK_ROOT) { Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe" }
    (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

$adb = $adbCandidates | Select-Object -First 1
if (-not $adb) { throw "adb was not found. Install Android SDK Platform-Tools." }
if (-not (Test-Path -LiteralPath $ApkPath)) { throw "APK not found: $ApkPath" }

& $adb devices
& $adb install -r -t $ApkPath
if ($LASTEXITCODE -ne 0) { throw "APK installation failed. Check USB debugging and the device authorization prompt." }
