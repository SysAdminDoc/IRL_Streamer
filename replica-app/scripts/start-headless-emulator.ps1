[CmdletBinding()]
param(
    [string]$Avd = 'issue-sweep-api36',
    [int]$BootTimeoutSeconds = 240
)

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
if (-not (Test-Path -LiteralPath $android.Emulator -PathType Leaf)) { throw "Emulator not found: $($android.Emulator)" }
$log = New-ValidationLog 'headless-emulator'
$errorLog = [IO.Path]::ChangeExtension($log, '.err.log')
$before = @(& $android.Adb devices | Select-String '^emulator-\d+\s' | ForEach-Object { ($_ -split '\s+')[0] })
$arguments = @('-avd', $Avd, '-no-window', '-no-audio', '-no-boot-anim', '-gpu', 'swiftshader_indirect', '-no-snapshot-load', '-no-snapshot-save')
$process = Start-Process -FilePath $android.Emulator -ArgumentList $arguments -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $errorLog -PassThru
Write-Host "Started hidden emulator process $($process.Id); waiting for an isolated serial."
$deadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
$serial = $null
while ((Get-Date) -lt $deadline -and $null -eq $serial) {
    Start-Sleep -Seconds 2
    $devices = @(& $android.Adb devices | Select-String '^emulator-\d+\s+device' | ForEach-Object { ($_ -split '\s+')[0] })
    $serial = $devices | Where-Object { $before -notcontains $_ } | Select-Object -First 1
}
if ($null -eq $serial) { throw "Headless emulator did not become ready within $BootTimeoutSeconds seconds. Logs: $log" }

$bootDeadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
do {
    Start-Sleep -Seconds 2
    $booted = (& $android.Adb -s $serial shell getprop sys.boot_completed 2>$null).Trim()
} while ($booted -ne '1' -and (Get-Date) -lt $bootDeadline)
if ($booted -ne '1') { throw "Emulator $serial did not complete boot." }

& $android.Adb -s $serial shell wm size 1080x2316 | Out-Null
& $android.Adb -s $serial shell wm density 450 | Out-Null
& $android.Adb -s $serial shell settings put system accelerometer_rotation 0 | Out-Null
& $android.Adb -s $serial shell settings put system user_rotation 1 | Out-Null
& $android.Adb -s $serial shell settings put system font_scale 1.0 | Out-Null
& $android.Adb -s $serial shell settings put secure immersive_mode_confirmations confirmed | Out-Null
& $android.Adb -s $serial shell cmd uimode night yes | Out-Null
& $android.Adb -s $serial shell cmd overlay enable com.android.internal.systemui.navbar.threebutton 2>$null | Out-Null
Write-Host "HEADLESS_EMULATOR_SERIAL=$serial"
