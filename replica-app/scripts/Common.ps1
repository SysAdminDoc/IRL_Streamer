Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$script:ProjectRoot = Split-Path -Parent $PSScriptRoot
$script:ValidationRoot = Join-Path $script:ProjectRoot 'validation'
$script:LogsRoot = Join-Path $script:ValidationRoot 'logs'

function Initialize-AndroidEnvironment {
    $sdk = [Environment]::GetEnvironmentVariable('ANDROID_HOME')
    if ([string]::IsNullOrWhiteSpace($sdk) -or -not (Test-Path -LiteralPath (Join-Path $sdk 'platform-tools\adb.exe') -PathType Leaf)) {
        $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    }

    $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME')
    if ([string]::IsNullOrWhiteSpace($javaHome) -or -not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe') -PathType Leaf)) {
        $javaHome = 'C:\Program Files\Android\Android Studio\jbr'
    }

    $adb = Join-Path $sdk 'platform-tools\adb.exe'
    $emulator = Join-Path $sdk 'emulator\emulator.exe'
    $java = Join-Path $javaHome 'bin\java.exe'
    foreach ($required in @($adb, $java)) {
        if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
            throw "Required executable was not found: $required"
        }
    }

    $env:ANDROID_HOME = $sdk
    $env:JAVA_HOME = $javaHome
    $env:PATH = "$(Join-Path $javaHome 'bin');$(Join-Path $sdk 'platform-tools');$env:PATH"
    return [pscustomobject]@{
        Sdk = $sdk
        JavaHome = $javaHome
        Adb = $adb
        Emulator = $emulator
        Gradle = Join-Path $script:ProjectRoot 'gradlew.bat'
    }
}

function New-ValidationLog {
    param([Parameter(Mandatory = $true)][string]$Name)
    New-Item -ItemType Directory -Path $script:LogsRoot -Force | Out-Null
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    return Join-Path $script:LogsRoot "$Name-$stamp.log"
}

# Run a native command and return its combined output as trimmed text, without
# letting stderr become a terminating error. Under the script-wide 'Stop'
# preference a bare `& native 2>&1` throws on the first stderr line, which turns
# routine adb noise ("error: device offline" during boot) into a crash.
function Get-NativeOutput {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [string[]]$Arguments = @()
    )
    $previousErrorAction = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & $FilePath @Arguments 2>&1
        return ($output | Out-String).Trim()
    }
    catch {
        return ''
    }
    finally {
        $ErrorActionPreference = $previousErrorAction
    }
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [string[]]$Arguments = @(),
        [string]$LogPath,
        [string]$WorkingDirectory = $script:ProjectRoot
    )

    if (-not (Get-Command -Name $FilePath -ErrorAction SilentlyContinue)) {
        throw "Required executable was not found on PATH or at that path: $FilePath"
    }
    Write-Host ("> {0} {1}" -f $FilePath, ($Arguments -join ' '))
    Push-Location $WorkingDirectory
    $previousErrorAction = $ErrorActionPreference
    # Initialized so a launch failure cannot surface as "the variable '$output'
    # cannot be retrieved because it has not been set", which hides the real cause.
    $output = @()
    try {
        # Windows PowerShell 5.1 wraps native stderr as ErrorRecord objects. Keep it
        # in the command log and use the native exit code as the actual gate.
        $ErrorActionPreference = 'Continue'
        $output = & $FilePath @Arguments 2>&1
        $exitCode = $LASTEXITCODE
        foreach ($line in $output) {
            Write-Host $line
            if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
                [string]$line | Out-File -LiteralPath $LogPath -Append -Encoding utf8
            }
        }
        if ($exitCode -ne 0) {
            throw "Command failed with exit code $($exitCode): $FilePath"
        }
    }
    finally {
        $ErrorActionPreference = $previousErrorAction
        Pop-Location
    }
}

function Assert-ReplicaDevice {
    param(
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string]$Adb,
        [switch]$AllowPhysicalDevice
    )
    if ([string]::IsNullOrWhiteSpace($Serial)) {
        throw 'A replica device serial is required. The physical audit phone must never be selected implicitly.'
    }
    $state = Get-NativeOutput -FilePath $Adb -Arguments @('-s', $Serial, 'get-state')
    if ($state -ne 'device') {
        throw "Replica device '$Serial' is not connected and ready."
    }
    if ($AllowPhysicalDevice) { return }

    # Replica QA must never run on the physical audit phone, and both it and the
    # emulator are routinely attached at once. Requiring a serial is not a guard
    # when a single mistyped or pasted serial is a valid connected device, so
    # confirm the target actually is an emulator before anything installs,
    # launches or clears data on it.
    $isEmulator = $Serial -like 'emulator-*'
    if (-not $isEmulator) {
        $qemu = Get-NativeOutput -FilePath $Adb -Arguments @('-s', $Serial, 'shell', 'getprop', 'ro.kernel.qemu')
        $isEmulator = ($qemu -eq '1')
    }
    if (-not $isEmulator) {
        throw "Refusing to target '$Serial': it does not report itself as an emulator. Replica QA must not run on the physical audit phone. Pass -AllowPhysicalDevice only with explicit authorization."
    }
}

function Stop-GradleDaemons {
    param([Parameter(Mandatory = $true)][pscustomobject]$Environment)
    # Best-effort cleanup. This runs from `finally` blocks, so it must not throw:
    # under the script-wide 'Stop' preference any JVM stderr noise (for example
    # "Picked up _JAVA_OPTIONS") would fail a green build, and would replace the
    # real exception when the try block had already thrown.
    $previousErrorAction = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $Environment.Gradle --stop --no-configuration-cache --console=plain 2>&1 | Out-Null
    }
    catch {
        Write-Host "Gradle daemon stop reported: $($_.Exception.Message)"
    }
    finally {
        $ErrorActionPreference = $previousErrorAction
    }
}
