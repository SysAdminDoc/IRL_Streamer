[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
$log = New-ValidationLog 'build-debug'
try {
    Invoke-Checked -FilePath $android.Gradle -Arguments @(':app:assembleDebug', '--no-daemon', '--no-configuration-cache', '--console=plain') -LogPath $log
}
finally {
    Stop-GradleDaemons -Environment $android
}
Write-Host "Debug APK: $(Join-Path $script:ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk')"
