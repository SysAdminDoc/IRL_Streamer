[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$ScreenId)

. (Join-Path $PSScriptRoot 'Common.ps1')
$safeId = [IO.Path]::GetFileNameWithoutExtension($ScreenId)
if ($safeId -ne $ScreenId -or $safeId -notmatch '^\d{3}_[A-Za-z0-9_-]+$') { throw "Invalid screen ID: $ScreenId" }
$baseline = Join-Path $script:ValidationRoot "baseline\$safeId.png"
$current = Join-Path $script:ValidationRoot "current\$safeId.png"
foreach ($path in @($baseline, $current)) { if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Image not found: $path" } }

$thresholdRow = Import-Csv -LiteralPath (Join-Path $script:ValidationRoot 'thresholds.csv') | Where-Object { $_.screen_id -eq $safeId } | Select-Object -First 1
if ($null -eq $thresholdRow) { throw "No threshold configured for $safeId" }
$mask = Join-Path $script:ValidationRoot "masks\$safeId.png"
$arguments = @(
    '-3.12', (Join-Path $PSScriptRoot 'visual_compare.py'),
    '--baseline', $baseline,
    '--current', $current,
    '--side-by-side', (Join-Path $script:ValidationRoot "side-by-side\$safeId.png"),
    '--overlay', (Join-Path $script:ValidationRoot "overlays\$safeId.png"),
    '--diff', (Join-Path $script:ValidationRoot "diffs\$safeId.png"),
    '--result', (Join-Path $script:ValidationRoot "results\$safeId.json"),
    '--threshold', $thresholdRow.ssim_threshold,
    '--max-shift', $thresholdRow.max_alignment_px
)
if (Test-Path -LiteralPath $mask -PathType Leaf) { $arguments += @('--mask', $mask) }
& py.exe @arguments
exit $LASTEXITCODE
