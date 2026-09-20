# Brand assets

| File | Use |
|---|---|
| `logo.svg` | Full lockup (mark + wordmark), light backgrounds |
| `logo-dark.svg` | Full lockup, dark backgrounds |
| `logo-mark.svg` | Mark only, square — avatars, favicons, social preview |
| `logo-mark-dark.svg` | Mark only, dark backgrounds |

The SVGs are the source. `png/` holds rasterized versions of them plus two
composed cards — a 1280×640 GitHub social preview and a 1200×627 card for
social posts. Those are derived artifacts: edit an SVG, then re-run
`./generate-png.sh [version]` (needs `rsvg-convert` and `magick`), never hand-edit
a PNG.

The mark is an open **C** with a check breaking out of its opening: a Cassandra
table, and the assertion that says it holds what it should.

## Palette

| Token | Light | Dark | Use |
|---|---|---|---|
| ink | `#0B2A38` | `#EAF4F8` | wordmark — "Cassandra" |
| primary | `#1287A8` | `#4FB6D4` | mark body, wordmark — "Unit" |
| accent | `#37C2B4` | `#52D8C8` | the check |

## Usage

The mark is drawn on a 32×32 grid and stays legible down to 16px. All of its ink
sits inside the circle inscribed in that grid, so it survives a circular avatar
crop with nothing clipped — use `logo-mark.svg` as-is for avatars and favicons.

When placing the mark in a layout, leave clear space around it of at least a
quarter of its height. Don't recolour, rotate, or stretch it; for single-colour
contexts, drop the accent and draw the whole mark in `primary`.

## Typography

The wordmark is [Inter Tight](https://fonts.google.com/specimen/Inter+Tight)
SemiBold, converted to outlines — the SVGs contain no `<text>` and need no font
installed. Inter Tight is © 2022 The Inter Project Authors, licensed under the
SIL Open Font License 1.1.

## Trademark

This mark is original to CassandraUnit and is not affiliated with or endorsed by
the Apache Software Foundation. Apache Cassandra is a trademark of the ASF; the
palette is in a related hue family, but no ASF mark is reproduced here.
