[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$Serial)

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
Assert-ReplicaDevice -Serial $Serial -Adb $android.Adb
$apk = Join-Path $script:ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw "Debug APK not found: $apk" }
$log = New-ValidationLog 'install-debug'
Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'install', '-r', $apk) -LogPath $log
Write-Host "Installed com.irlstreamer.reconstruction.debug on $Serial"
