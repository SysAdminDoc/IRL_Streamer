[CmdletBinding()]
param(
    # Rewrites gradle\verification-metadata.xml from what this build resolves.
    # Only pass this when a dependency was deliberately added or upgraded, and
    # review the diff: it is the file that would otherwise catch a swapped artifact.
    [switch]$RegenerateVerification
)

. (Join-Path $PSScriptRoot 'Common.ps1')

$requiredSigningVariables = @(
    'IRL_STREAMER_KEYSTORE_FILE',
    'IRL_STREAMER_KEYSTORE_PASSWORD',
    'IRL_STREAMER_KEY_ALIAS',
    'IRL_STREAMER_KEY_PASSWORD'
)
$missingSigningVariables = @($requiredSigningVariables | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_))
})
if ($missingSigningVariables.Count -gt 0) {
    throw "Release signing is not configured. Set: $($missingSigningVariables -join ', ')"
}
$signingStore = [Environment]::GetEnvironmentVariable('IRL_STREAMER_KEYSTORE_FILE')
if (-not (Test-Path -LiteralPath $signingStore -PathType Leaf)) {
    throw "Release signing store was not found: $signingStore"
}

$android = Initialize-AndroidEnvironment
$log = New-ValidationLog 'build-release'
$common = @('--no-daemon', '--no-configuration-cache', '--console=plain')
try {
    if ($RegenerateVerification) {
        Write-Host 'Rewriting dependency verification metadata; review the diff before committing.'
        # Every configuration the repo's gates resolve, or the regenerated file
        # is missing the test and lint artifacts and those tasks fail closed.
        $arguments = @(
            '--write-verification-metadata', 'sha256',
            ':app:assembleDebug', ':app:assembleRelease',
            ':app:testDebugUnitTest', ':app:testReleaseUnitTest',
            ':app:lint', ':app:compileDebugAndroidTestKotlin'
        ) + $common
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
