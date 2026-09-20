#!/usr/bin/env bash
#
# Rasterizes the brand SVGs into docs/assets/png/, and composes the two social
# cards. The PNGs are derived artifacts -- edit the SVGs, then re-run this.
#
#   ./generate-png.sh [version]
#
# Version defaults to the latest release tag. Requires rsvg-convert and magick.

set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
out="$here/png"

version="${1:-}"
if [[ -z "$version" ]]; then
    version="$(git -C "$here" describe --tags --abbrev=0 2>/dev/null \
        | sed 's/^cassandra-unit-parent-//')"
    version="${version:-5.2.0}"
fi

for tool in rsvg-convert magick; do
    command -v "$tool" >/dev/null || { echo "$tool not found" >&2; exit 1; }
done

# Palette, from README.md. Keep these in step with the SVGs.
ink="#0B2A38"
primary_dark="#4FB6D4"
tagline_fill="#9FC4D2"

tagline="Test fixtures and assertions for Apache Cassandra"

mkdir -p "$out"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

echo "==> mark, square"
for size in 32 64 128 256 512; do
    rsvg-convert -w "$size" -h "$size" "$here/logo-mark.svg"      -o "$out/logo-mark-$size.png"
    rsvg-convert -w "$size" -h "$size" "$here/logo-mark-dark.svg" -o "$out/logo-mark-dark-$size.png"
done

echo "==> lockup, 1x/2x/3x of 300.2x48, on transparency"
for width in 300 600 900; do
    rsvg-convert -w "$width" "$here/logo.svg"      -o "$out/logo-$width.png"
    rsvg-convert -w "$width" "$here/logo-dark.svg" -o "$out/logo-dark-$width.png"
done

echo "==> GitHub social preview, 1280x640"
rsvg-convert -w 780 "$here/logo-dark.svg" -o "$tmp/lockup-780.png"
magick -size 1280x640 "xc:$ink" "$tmp/lockup-780.png" -gravity center -composite \
    "$out/social-preview-1280x640.png"

echo "==> LinkedIn card, 1200x627"
# The lockup is rendered first and embedded as a raster: librsvg handles PNG in
# <image> reliably, SVG in <image> does not.
rsvg-convert -w 840 "$here/logo-dark.svg" -o "$tmp/lockup-840.png"

# 840 / 300.2 * 48 = 134.3. Centred at x=180, sitting above the middle so the
# lockup and the tagline read as one block.
cat > "$tmp/card.svg" <<SVG
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
     width="1200" height="627" viewBox="0 0 1200 627">
  <rect width="1200" height="627" fill="$ink"/>
  <image xlink:href="lockup-840.png" x="180" y="210" width="840" height="134.3"/>
  <text x="600" y="420" text-anchor="middle"
        font-family="Inter Tight, Helvetica Neue, Helvetica, sans-serif"
        font-size="34" fill="$tagline_fill">$tagline</text>
  <text x="1152" y="579" text-anchor="end"
        font-family="Inter Tight, Helvetica Neue, Helvetica, sans-serif"
        font-size="24" fill="$primary_dark">$version</text>
</svg>
SVG

rsvg-convert -w 1200 -h 627 "$tmp/card.svg" -o "$out/linkedin-card-1200x627.png"

echo
echo "wrote $(find "$out" -name '*.png' | wc -l | tr -d ' ') files to $out (version $version)"
