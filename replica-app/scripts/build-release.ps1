[CmdletBinding()]
param(
    # Rewrites gradle\verification-metadata.xml from what this build resolves.
    # Only pass this when a dependency was deliberately added or upgraded, and
    # review the diff: it is the file that would otherwise catch a swapped artifact.
    [switch]$RegenerateVerification
)

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
$log = New-ValidationLog 'build-release'
$common = @('--no-daemon', '--no-configuration-cache', '--console=plain')
try {
    if ($RegenerateVerification) {
        Write-Host 'Rewriting dependency verification metadata; review the diff before committing.'
        $arguments = @('--write-verification-metadata', 'sha256', ':app:assembleRelease') + $common
    }
    else {
        $arguments = @(':app:assembleRelease') + $common
    }
    Invoke-Checked -FilePath $android.Gradle -Arguments $arguments -LogPath $log
}
finally {
    Stop-GradleDaemons -Environment $android
}
Write-Host "Minified release APK: $(Join-Path $script:ProjectRoot 'app\build\outputs\apk\release\app-release.apk')"
