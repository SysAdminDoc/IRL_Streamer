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
    # A wrong -Avd name exits within seconds. Without this the script would block
    # for the whole timeout and then blame the timeout rather than the emulator.
    if ($process.HasExited) {
        throw "Emulator process exited with code $($process.ExitCode) before reporting a serial. Check $errorLog (a wrong -Avd name is the usual cause; this run used '$Avd')."
    }
    $devices = @(Get-NativeOutput -FilePath $android.Adb -Arguments @('devices') |
        Select-String '^emulator-\d+\s+device' | ForEach-Object { ($_ -split '\s+')[0] })
    $serial = $devices | Where-Object { $before -notcontains $_ } | Select-Object -First 1
}
if ($null -eq $serial) { throw "Headless emulator did not become ready within $BootTimeoutSeconds seconds. Logs: $log" }

$bootDeadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
$booted = ''
do {
    Start-Sleep -Seconds 2
    if ($process.HasExited) {
        throw "Emulator process exited with code $($process.ExitCode) during boot. Check $errorLog."
    }
    # adb writes "error: device offline"/"error: closed" to stderr during this
    # window. Get-NativeOutput keeps that from becoming a terminating error and
    # returns '' rather than $null, so the comparison below is always safe.
    $booted = Get-NativeOutput -FilePath $android.Adb -Arguments @('-s', $serial, 'shell', 'getprop', 'sys.boot_completed')
} while ($booted -ne '1' -and (Get-Date) -lt $bootDeadline)
if ($booted -ne '1') { throw "Emulator $serial did not complete boot." }

foreach ($settingArgs in @(
    @('shell', 'wm', 'size', '1080x2316'),
    @('shell', 'wm', 'density', '450'),
    # Animations are the largest remaining source of capture-to-capture pixel
    # noise: a screenshot taken while a transition is in flight differs from the
    # same screen at rest. All three scales must be zero; setting only one leaves
    # the others running.
    @('shell', 'settings', 'put', 'global', 'window_animation_scale', '0'),
    @('shell', 'settings', 'put', 'global', 'transition_animation_scale', '0'),
    @('shell', 'settings', 'put', 'global', 'animator_duration_scale', '0'),
    @('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0'),
    @('shell', 'settings', 'put', 'system', 'user_rotation', '1'),
    @('shell', 'settings', 'put', 'system', 'font_scale', '1.0'),
    @('shell', 'settings', 'put', 'secure', 'immersive_mode_confirmations', 'confirmed'),
    @('shell', 'cmd', 'uimode', 'night', 'yes'),
    @('shell', 'cmd', 'overlay', 'enable', 'com.android.internal.systemui.navbar.threebutton')
)) {
    Get-NativeOutput -FilePath $android.Adb -Arguments (@('-s', $serial) + $settingArgs) | Out-Null
}

# Read the display back. A silently failed `wm size` produces 145 dimension
# mismatches downstream with nothing pointing at the cause.
$sizeReport = Get-NativeOutput -FilePath $android.Adb -Arguments @('-s', $serial, 'shell', 'wm', 'size')
$densityReport = Get-NativeOutput -FilePath $android.Adb -Arguments @('-s', $serial, 'shell', 'wm', 'density')
if ($sizeReport -notmatch '1080x2316') {
    throw "Emulator $serial reports '$sizeReport' instead of the audited 1080x2316. Captures would not match the baselines."
}
if ($densityReport -notmatch '450') {
    throw "Emulator $serial reports '$densityReport' instead of the audited density 450."
}
Write-Host "Display verified: $sizeReport / $densityReport"

# SystemUI demo mode (a frozen clock and battery) is deliberately NOT enabled.
# It would stabilise the status bar across runs, but the strict gate compares
# unmasked whole screens against a Samsung phone that was not in demo mode, so
# it would move every settings capture further from its baseline for no parity
# gain. The status bar is instead excluded from the secondary app-chrome metric
# only (validation/masks/mask-register.csv).
Write-Host "HEADLESS_EMULATOR_SERIAL=$serial"
