[CmdletBinding()]
param(
    [string]$Serial = '',
    [double]$WithinPxTarget = 2.0
)

# Layout-bounds gate. SSIM answers "do these screens look alike"; this answers
# "is each element where the audit says it is", which is the target the audit
# states in pixels. It compares the immutable audit UI hierarchy against the
# replica hierarchy captured alongside every screenshot.

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

$rows = Import-Csv -LiteralPath $summaryCsv | Where-Object { $_.status -eq 'OK' }
if ($rows.Count -eq 0) { throw "Geometry comparison produced no rows." }

$meanErr = ($rows | ForEach-Object { [double]$_.mean_origin_err_px } | Measure-Object -Average).Average
$within2 = ($rows | ForEach-Object { [double]$_.origins_within_2px_pct } | Measure-Object -Average).Average
$within4 = ($rows | ForEach-Object { [double]$_.origins_within_4px_pct } | Measure-Object -Average).Average
$clean = @($rows | Where-Object { [double]$_.origins_within_2px_pct -ge 95 }).Count

$reportPath = Join-Path $script:ValidationRoot 'reports\geometry-validation-report.md'
$report = @(
    '# Geometry validation report',
    '',
    "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')",
    "States compared: $($rows.Count)",
    "Target: element origins within $WithinPxTarget px",
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
    '',
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
