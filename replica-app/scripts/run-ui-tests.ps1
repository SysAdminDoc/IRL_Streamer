[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$Serial)

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
Assert-ReplicaDevice -Serial $Serial -Adb $android.Adb
$env:ANDROID_SERIAL = $Serial
$log = New-ValidationLog 'ui-tests'
try {
    Invoke-Checked -FilePath $android.Gradle -Arguments @(':app:connectedDebugAndroidTest', '--no-daemon', '--no-configuration-cache', '--console=plain') -LogPath $log
}
finally {
    Stop-GradleDaemons -Environment $android
}
Write-Host "Instrumentation tests passed on the explicitly selected replica device $Serial. Log: $log"
