[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
$log = New-ValidationLog 'unit-tests'
try {
    Invoke-Checked -FilePath $android.Gradle -Arguments @(':app:testDebugUnitTest', ':app:testReleaseUnitTest', '--no-daemon', '--no-configuration-cache', '--console=plain') -LogPath $log
}
finally {
    Stop-GradleDaemons -Environment $android
}
Write-Host "Unit tests passed. Log: $log"
