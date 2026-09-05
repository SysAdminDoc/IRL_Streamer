[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [switch]$AllScreens
)

$ErrorActionPreference = 'Stop'
$steps = @(
    @{ Script = 'check-environment.ps1'; Args = @('-Serial', $Serial) },
    @{ Script = 'build-debug.ps1'; Args = @() },
    @{ Script = 'run-unit-tests.ps1'; Args = @() },
    @{ Script = 'install-debug.ps1'; Args = @('-Serial', $Serial) },
    @{ Script = 'run-ui-tests.ps1'; Args = @('-Serial', $Serial) },
    # connectedDebugAndroidTest may remove the target package after instrumentation.
    @{ Script = 'install-debug.ps1'; Args = @('-Serial', $Serial) }
)
foreach ($step in $steps) {
    $childArguments = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot $step.Script)) + @($step.Args)
    & powershell.exe @childArguments
    if ($LASTEXITCODE -ne 0) { throw "$($step.Script) failed with exit code $LASTEXITCODE" }
}

# Regenerate the documented comparison masks before comparing, so the secondary
# app-chrome metric always reflects the current mask register.
$python = Resolve-PythonCommand
& $python.Path @($python.Prefix + @((Join-Path $PSScriptRoot 'build_masks.py')))
if ($LASTEXITCODE -ne 0) { throw "Mask generation failed." }

$visualArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot 'run-visual-validation.ps1'), '-Serial', $Serial)
if ($AllScreens) { $visualArgs += '-All' }
& powershell.exe @visualArgs
$visualExit = $LASTEXITCODE

# The geometry gate runs on the hierarchy dumps captured beside each screenshot,
# so it must run after the visual sweep even when that sweep reported failures.
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'run-geometry-validation.ps1') -Serial $Serial
if ($LASTEXITCODE -ne 0) { throw "Geometry validation failed to run." }

if ($visualExit -ne 0) { throw "Visual validation completed with failures. See validation/reports/visual-validation-report.md" }
Write-Host 'Full validation passed.'
