[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
$log = New-ValidationLog 'build-release'
try {
    Invoke-Checked -FilePath $android.Gradle -Arguments @(':app:assembleRelease', '--no-daemon', '--no-configuration-cache', '--console=plain') -LogPath $log
}
finally {
    Stop-GradleDaemons -Environment $android
}
Write-Host "Minified release APK: $(Join-Path $script:ProjectRoot 'app\build\outputs\apk\release\app-release.apk')"
