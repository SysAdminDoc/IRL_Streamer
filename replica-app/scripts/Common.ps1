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

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [string[]]$Arguments = @(),
        [string]$LogPath,
        [string]$WorkingDirectory = $script:ProjectRoot
    )

    Write-Host ("> {0} {1}" -f $FilePath, ($Arguments -join ' '))
    Push-Location $WorkingDirectory
    $previousErrorAction = $ErrorActionPreference
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
        [Parameter(Mandatory = $true)][string]$Adb
    )
    if ([string]::IsNullOrWhiteSpace($Serial)) {
        throw 'A replica device serial is required. The physical audit phone must never be selected implicitly.'
    }
    $state = & $Adb -s $Serial get-state 2>&1
    if ($LASTEXITCODE -ne 0 -or ($state -join '').Trim() -ne 'device') {
        throw "Replica device '$Serial' is not connected and ready."
    }
}

function Stop-GradleDaemons {
    param([Parameter(Mandatory = $true)][pscustomobject]$Environment)
    & $Environment.Gradle --stop --no-configuration-cache --console=plain 2>&1 | Out-Null
}
