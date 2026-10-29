# Bundled circle flags

- Upstream: https://github.com/HatScripts/circle-flags
- Branch: `gh-pages`
- Pinned commit: `379588b5da95482d6bbf10bd45644a35b0609ea6`
- Archive: `country-flags.zip`, containing all 444 upstream SVG files.
- License: MIT; see `LICENSE.md`.

The app copies the archive to private cache storage once, opens it with
`ZipFile` for indexed lookup, and streams only the requested entry into the SVG
decoder. Unknown or missing country codes fall back locally to `other/earth.svg`.
There is no network fallback.
