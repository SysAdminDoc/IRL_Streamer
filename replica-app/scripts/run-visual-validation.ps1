[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [string[]]$ScreenId = @(),
    [switch]$All
)

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
Assert-ReplicaDevice -Serial $Serial -Adb $android.Adb
$catalogPath = Join-Path (Split-Path $script:ProjectRoot -Parent) 'app-audit\screens\screen-catalog.csv'
$catalog = Import-Csv -LiteralPath $catalogPath
if ($All) {
    $targets = @($catalog.screen_id)
}
elseif ($ScreenId.Count -gt 0) {
    $targets = @($ScreenId)
}
else {
    $representative = @(1, 2, 21, 32, 65, 76, 83, 91, 108, 120, 129, 130, 132, 135, 138, 142)
    $targets = @($catalog | Where-Object { $representative -contains [int]$_.screen_id.Substring(0, 3) } | ForEach-Object { $_.screen_id })
}

$rows = @()
$targetIndex = 0
foreach ($id in $targets) {
    Write-Host "Validating $id"
    $captureArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot 'capture-replica-screen.ps1'), '-Serial', $Serial, '-ScreenId', $id)
    if ($targetIndex -gt 0) { $captureArgs += '-Warm' }
    & powershell.exe @captureArgs
    $captureExit = $LASTEXITCODE
    $compareExit = 99
    if ($captureExit -eq 0) {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'compare-screen.ps1') -ScreenId $id
        $compareExit = $LASTEXITCODE
    }

    $resultPath = Join-Path $script:ValidationRoot "results\$id.json"
    if (Test-Path -LiteralPath $resultPath -PathType Leaf) {
        $result = Get-Content -LiteralPath $resultPath -Raw | ConvertFrom-Json
        $rows += [pscustomobject]@{ Screen = $id; Status = $result.status; SSIM = [double]$result.ssim; Threshold = [double]$result.threshold; Shift = "$($result.alignment.dx),$($result.alignment.dy)" }
    }
    else {
        $rows += [pscustomobject]@{ Screen = $id; Status = 'ERROR'; SSIM = 0.0; Threshold = 0.985; Shift = '-' }
    }
    $targetIndex++
}

$passed = @($rows | Where-Object { $_.Status -eq 'PASS' }).Count
$failed = $rows.Count - $passed
$reportPath = Join-Path $script:ValidationRoot 'reports\visual-validation-report.md'
$report = @(
    '# Visual validation report',
    '',
    "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')",
    "Replica device: ``$Serial``",
    "States captured: $($rows.Count)",
    "Passed configured threshold: $passed",
    "Below threshold or errored: $failed",
    '',
    '| Screen | Status | SSIM | Threshold | Alignment dx,dy |',
    '|---|---:|---:|---:|---:|'
)
foreach ($row in $rows) { $report += "| $($row.Screen) | $($row.Status) | $([math]::Round($row.SSIM, 6)) | $($row.Threshold) | $($row.Shift) |" }
$report += @('', 'A failed metric is not masked or waived automatically. Inspect the matching files in `side-by-side/`, `overlays/`, and `diffs/` and record any legitimate platform-only variance before changing a threshold.')
$report | Set-Content -LiteralPath $reportPath -Encoding utf8
$rows | Export-Csv -LiteralPath (Join-Path $script:ValidationRoot 'reports\visual-validation-results.csv') -NoTypeInformation -Encoding utf8
Write-Host "Visual validation report: $reportPath"
if ($failed -gt 0) { exit 2 }
