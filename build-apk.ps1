param(
    [string]$AiBaseUrl = "http://10.0.2.2:8787"
)

$ErrorActionPreference = "Stop"
$temporaryDrive = $null
try {
    $localTools = Join-Path $PSScriptRoot ".tools"
    if (Test-Path -LiteralPath $localTools) {
        $driveLetter = @("T", "U", "V", "W") | Where-Object { -not (Test-Path -LiteralPath "${_}:\") } | Select-Object -First 1
        if (-not $driveLetter) { throw "No free temporary drive letter is available for the local Android toolchain." }
        subst "${driveLetter}:" $localTools
        $temporaryDrive = $driveLetter
        $jdk = Get-ChildItem -LiteralPath "${driveLetter}:\jdk" -Directory | Select-Object -First 1
        if (-not $jdk) { throw "Local JDK was not found under .tools\jdk." }
        $env:JAVA_HOME = $jdk.FullName
        $env:ANDROID_SDK_ROOT = "${driveLetter}:\android-sdk"
    } elseif (-not $env:JAVA_HOME -or -not $env:ANDROID_SDK_ROOT) {
        throw "Install Android Studio or configure JAVA_HOME and ANDROID_SDK_ROOT. See TESTING_GUIDE.md."
    }

    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
    $wrapperJar = Join-Path $PSScriptRoot "gradle\wrapper\gradle-wrapper.jar"
    & $java -classpath $wrapperJar org.gradle.wrapper.GradleWrapperMain testDebugUnitTest lintDebug assembleDebug "-PAI_BASE_URL=$AiBaseUrl" --console=plain --no-daemon
    $gradleExit = $LASTEXITCODE
    if ($gradleExit -ne 0) { throw "Gradle build failed with exit code $gradleExit." }

    $apk = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
    $hash = Get-FileHash -LiteralPath $apk -Algorithm SHA256
    Write-Output "APK: $apk"
    Write-Output "SHA256: $($hash.Hash)"
    Write-Output "AI backend: $AiBaseUrl"
} finally {
    if ($temporaryDrive) { subst "${temporaryDrive}:" /d }
}
