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

# Baselines are the audit's own screenshots and are gitignored, so a fresh clone
# has none and every compare would fail with "Image not found". Restore them
# before capturing rather than making that a documented manual step.
$baselineDir = Join-Path $script:ValidationRoot 'baseline'
if (-not (Test-Path -LiteralPath $baselineDir) -or @(Get-ChildItem -LiteralPath $baselineDir -Filter '*.png' -ErrorAction SilentlyContinue).Count -eq 0) {
    Write-Host 'No comparison baselines present; restoring them from the audit evidence.'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'sync-baselines.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Could not restore comparison baselines from the audit.' }
}
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

    # Trust the result JSON only when this run actually produced it. The capture
    # step deletes the previous artifacts up front, so a missing file here means
    # this run failed rather than that a previous run passed.
    # visual_compare.py exits 0 (PASS), 2 (below threshold) and 3 (dimension
    # mismatch) after writing a result; any other code means it never got there.
    $resultPath = Join-Path $script:ValidationRoot "results\$id.json"
    if ($captureExit -ne 0) {
        $rows += [pscustomobject]@{ Screen = $id; Status = 'CAPTURE_FAILED'; SSIM = 0.0; Threshold = 0.985; Shift = '-' }
    }
    elseif ($compareExit -notin @(0, 2, 3)) {
        $rows += [pscustomobject]@{ Screen = $id; Status = 'COMPARE_FAILED'; SSIM = 0.0; Threshold = 0.985; Shift = '-' }
    }
    elseif (Test-Path -LiteralPath $resultPath -PathType Leaf) {
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
