# Validation masks

White pixels exclude genuinely variable content; black pixels remain measurable. A mask must have exactly the same dimensions as its baseline and current screenshots.

No regions are currently masked. This is intentional: the first validation pass reports all differences, including system-clock and local preview differences. A mask may be added only with a screen-specific explanation here; implementation inaccuracies must never be masked.
