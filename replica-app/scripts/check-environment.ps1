[CmdletBinding()]
param([string]$Serial = '')

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
$log = New-ValidationLog 'check-environment'

"Project: $script:ProjectRoot" | Tee-Object -FilePath $log
"Android SDK: $($android.Sdk)" | Tee-Object -FilePath $log -Append
"JAVA_HOME: $($android.JavaHome)" | Tee-Object -FilePath $log -Append
Invoke-Checked -FilePath $android.Adb -Arguments @('version') -LogPath $log
Invoke-Checked -FilePath (Join-Path $android.JavaHome 'bin\java.exe') -Arguments @('-version') -LogPath $log
Invoke-Checked -FilePath $android.Gradle -Arguments @('--version', '--no-daemon', '--no-configuration-cache', '--console=plain') -LogPath $log

$pythonCheck = 'import PIL,numpy,skimage; print(PIL.__version__, numpy.__version__, skimage.__version__)'
$python = Resolve-PythonCommand
"Python: $($python.Path) $($python.Prefix -join ' ')" | Tee-Object -FilePath $log -Append
Invoke-Checked -FilePath $python.Path -Arguments ($python.Prefix + @('-c', $pythonCheck)) -LogPath $log

if (-not [string]::IsNullOrWhiteSpace($Serial)) {
    Assert-ReplicaDevice -Serial $Serial -Adb $android.Adb
    Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'shell', 'getprop', 'ro.build.version.release') -LogPath $log
}

Write-Host "Environment check passed. Log: $log"
