[CmdletBinding()]
param(
    [string]$Serial = '',
    [double]$WithinPxTarget = 2.0,
    [double]$RegressionTolerancePct = 1.0,
    [switch]$UpdateBaseline
)

# Layout-bounds gate. SSIM answers "do these screens look alike"; this answers
# "is each element where the audit says it is", which is the target the audit
# states in pixels. It compares the immutable audit UI hierarchy against the
# replica hierarchy captured alongside every screenshot.
#
# This gate can fail. It enforces three conditions:
#   1. Coverage    - every catalog state with audit evidence must be compared.
#   2. Non-vacuity - a state that matched zero elements is not a pass, it is a
#                    measurement that did not happen.
#   3. No regression - the aggregate accuracy may not fall below the recorded
#                    baseline. Absolute parity is a long-running goal, so the
#                    quality bar ratchets rather than being asserted outright.

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
$log = New-ValidationLog 'geometry-validation'

$hierarchyDir = Join-Path $script:ValidationRoot 'hierarchy'
if (-not (Test-Path -LiteralPath $hierarchyDir)) {
    throw "No replica hierarchy dumps found. Run capture-replica-screen.ps1 or run-visual-validation.ps1 first."
}

$summaryCsv = Join-Path $script:ValidationRoot 'reports\geometry-summary.csv'
Invoke-Checked -FilePath 'py.exe' -Arguments @(
    '-3.12', (Join-Path $PSScriptRoot 'geometry_diff.py'),
    '--all',
    '--replica-dir', $hierarchyDir,
    '--out-csv', $summaryCsv
) -LogPath $log

# Always wrap Where-Object results: under StrictMode 2.0 a zero- or one-row
# result has no .Count property and the guard below would throw instead of
# reporting the real problem.
$allRows = @(Import-Csv -LiteralPath $summaryCsv)
$rows = @($allRows | Where-Object { $_.status -eq 'OK' })
if ($allRows.Count -eq 0) { throw "Geometry comparison produced no rows." }

# Expected coverage is every catalog state that has audit evidence to compare
# against, not merely however many dumps happen to be on disk.
$auditXmlDir = Join-Path (Split-Path $script:ProjectRoot -Parent) 'app-audit\evidence\ui-xml'
$catalogPath = Join-Path (Split-Path $script:ProjectRoot -Parent) 'app-audit\screens\screen-catalog.csv'
$expected = @(Import-Csv -LiteralPath $catalogPath | Where-Object {
    Test-Path -LiteralPath (Join-Path $auditXmlDir "$($_.screen_id).xml") -PathType Leaf
})
$missingDumps = @($allRows | Where-Object { $_.status -ne 'OK' })
$vacuous = @($rows | Where-Object { [int]$_.matched -eq 0 })

$meanErr = ($rows | ForEach-Object { [double]$_.mean_origin_err_px } | Measure-Object -Average).Average
$within2 = ($rows | ForEach-Object { [double]$_.origins_within_2px_pct } | Measure-Object -Average).Average
$within4 = ($rows | ForEach-Object { [double]$_.origins_within_4px_pct } | Measure-Object -Average).Average
$clean = @($rows | Where-Object { [double]$_.origins_within_2px_pct -ge 95 }).Count
if ($null -eq $meanErr) { $meanErr = 0.0 }
if ($null -eq $within2) { $within2 = 0.0 }
if ($null -eq $within4) { $within4 = 0.0 }

$baselinePath = Join-Path $script:ValidationRoot 'geometry-baseline.json'
$baselineWithin2 = 0.0
if (Test-Path -LiteralPath $baselinePath -PathType Leaf) {
    $baselineWithin2 = [double](Get-Content -LiteralPath $baselinePath -Raw | ConvertFrom-Json).within_2px_pct
}

$failures = @()
if ($rows.Count -lt $expected.Count) {
    $failures += "Coverage: compared $($rows.Count) of $($expected.Count) states with audit evidence. Missing: $(@($missingDumps | ForEach-Object { $_.screen }) -join ', ')"
}
if ($vacuous.Count -gt 0) {
    $failures += "Vacuous: $($vacuous.Count) state(s) matched zero elements and measured nothing: $(@($vacuous | ForEach-Object { $_.screen }) -join ', ')"
}
if ($within2 -lt ($baselineWithin2 - $RegressionTolerancePct)) {
    $failures += ("Regression: {0:N1}% of origins within {1} px, below the recorded baseline {2:N1}% (tolerance {3:N1}%)." -f $within2, $WithinPxTarget, $baselineWithin2, $RegressionTolerancePct)
}

$reportPath = Join-Path $script:ValidationRoot 'reports\geometry-validation-report.md'
$report = @(
    '# Geometry validation report',
    '',
    "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')",
    "States compared: $($rows.Count) of $($expected.Count) with audit evidence",
    "Target: element origins within $WithinPxTarget px",
    "Recorded baseline: $([math]::Round($baselineWithin2, 1))% within $WithinPxTarget px",
    '',
    '## Method',
    '',
    'Each replica UI hierarchy is matched against the audit UI hierarchy for the same',
    'state. Nodes are paired by visible label, choosing the nearest candidate by origin',
    'so that repeated labels are not mispaired. Only the element origin (left, top) is',
    'scored: an Android `TextView` in a preference row stretches to the full row width',
    'while the equivalent Compose `Text` wraps its glyphs, so right and bottom edges are',
    'not comparable between the two toolkits and are reported for information only.',
    '',
    '## Result',
    '',
    '| Measure | Result |',
    '|---|---:|',
    "| Mean absolute origin error | $([math]::Round($meanErr, 2)) px |",
    "| Origins within 2 px | $([math]::Round($within2, 1))% |",
    "| Origins within 4 px | $([math]::Round($within4, 1))% |",
    "| States with >=95% of origins within 2 px | $clean / $($rows.Count) |",
    "| States that measured nothing | $($vacuous.Count) |",
    "| States with no replica dump | $($missingDumps.Count) |",
    ''
)
if ($failures.Count -gt 0) {
    $report += @('## Gate failures', '')
    foreach ($failure in $failures) { $report += "- $failure" }
    $report += ''
}
$report += @(
    '## Worst states',
    '',
    '| Screen | Matched | Unmatched | max dleft | max dtop | Mean origin error px | Within 2 px |',
    '|---|---:|---:|---:|---:|---:|---:|'
)
foreach ($row in ($rows | Sort-Object { [double]$_.mean_origin_err_px } -Descending | Select-Object -First 20)) {
    $report += "| $($row.screen) | $($row.matched) | $($row.unmatched) | $($row.max_dleft) | $($row.max_dtop) | $($row.mean_origin_err_px) | $($row.origins_within_2px_pct)% |"
}
$report += @('', 'Per-element detail for every state is in `validation/reports/geometry/<screen>.json`.')
$report | Set-Content -LiteralPath $reportPath -Encoding utf8

Write-Host "Geometry validation report: $reportPath"
Write-Host ("Mean origin error {0:N2} px; {1:N1}% of origins within 2 px; {2} clean states." -f $meanErr, $within2, $clean)

if ($UpdateBaseline) {
    if ($failures.Count -gt 0) { throw "Refusing to update the baseline while the gate is failing." }
    [ordered]@{
        recorded = (Get-Date -Format 'yyyy-MM-dd')
        within_2px_pct = [math]::Round($within2, 3)
        within_4px_pct = [math]::Round($within4, 3)
        mean_origin_error_px = [math]::Round($meanErr, 3)
        states_compared = $rows.Count
        note = 'Ratchet baseline for the geometry gate. Raise it by fixing layout, never by lowering this file.'
    } | ConvertTo-Json | Set-Content -LiteralPath $baselinePath -Encoding utf8
    Write-Host "Updated geometry baseline: $baselinePath"
}

if ($failures.Count -gt 0) {
    foreach ($failure in $failures) { Write-Host "GEOMETRY GATE FAILED: $failure" }
    exit 2
}
