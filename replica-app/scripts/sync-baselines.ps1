[CmdletBinding()]
param(
    [switch]$Force
)

# Populate validation/baseline/ from the immutable audit screenshots.
#
# The comparison baselines are the audit's own captures. They are generated
# evidence rather than source, so validation/baseline/ is gitignored - which
# means a fresh clone has no baselines at all and every documented compare
# command fails with "Image not found". This restores them from the audit.

. (Join-Path $PSScriptRoot 'Common.ps1')

$auditShots = Join-Path (Split-Path $script:ProjectRoot -Parent) 'app-audit\evidence\screenshots'
if (-not (Test-Path -LiteralPath $auditShots)) {
    throw "Audit screenshots not found: $auditShots"
}
$baselineDir = Join-Path $script:ValidationRoot 'baseline'
New-Item -ItemType Directory -Path $baselineDir -Force | Out-Null

$copied = 0
$skipped = 0
foreach ($source in Get-ChildItem -LiteralPath $auditShots -Filter '*.png') {
    $target = Join-Path $baselineDir $source.Name
    if ((Test-Path -LiteralPath $target -PathType Leaf) -and -not $Force) {
        $skipped++
        continue
    }
    Copy-Item -LiteralPath $source.FullName -Destination $target -Force
    $copied++
}

Write-Host "Baselines in ${baselineDir}: $copied copied, $skipped already present."
if ($copied -eq 0 -and $skipped -eq 0) {
    throw "No audit screenshots were found to copy."
}
