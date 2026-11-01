# What's Changed

- **Breaking changes: the minimum system version is now Android 13 (API 33); Android 12 and older cannot install or upgrade. The classic home and legacy home layout settings were removed**
- Added notification display settings with a live preview; the first line, second line, and right-side info can each show profile, status, node, traffic, expiry, or custom text
- Added Super Island notification settings using the two lines and right-side info; the island toggle and Shizuku status moved to notification settings
- Added Android 16 Live Updates with its own toggle, system status, and a "Select node" action
- Added in-app updates that download and install the APK, with Release / Nightly channels
- Added an update entry on the home screen
- Added crash logs on disk, copied to Downloads on the next launch
- Added anonymous usage analytics and a [public dashboard](https://analytics.yumebox.yumeyuka.moe/), reporting only a random device ID, Android version, manufacturer, and app version
- Added the [YumeBox download site](https://download.yumebox.yumeyuka.moe/); APKs and update manifests move to R2, GitHub Releases stays
- Rewrote the editor with indent guides, color preview, symbol bar, undo/redo/format/font-size menus, text selection menu, and unsaved-exit prompt
- Added direct editing, saving, and font resizing to the preview page
- Added the profile override page, rebuilt the profile editor and cards, and added sharing, enable toggle, expiry badge, and download progress
- Profile sorting now uses a bottom sheet
- Profile card traffic now uses a wave progress display
- Added app search, a standalone list, batch selection, package import/export, region quick-select, and app icons to access control
- Added overwrite/merge modes to backup restore, covering settings, subscriptions, overrides, and Sub-Store data
- Added an Extensions page and moved the remote controller out of the Lab
- Added search/sorting to connections and moved details to a standalone page
- The connections page shows a more accurate `host:port` target
- Added bottom-sheet editing and a permission flow to Wi-Fi automation
- Added a dynamic background, project/core source, log export, issue reporting, and licenses to About
- Added `yumebox://` deep links
- Added embedded Provider management to the rules page
- Added a custom theme color with `#RRGGBB` input
- Added wallpaper color extraction on the home screen; the accent color follows the wallpaper
- Rewrote all pages with a unified page shell, top bar, list, and popup components
- Added tab animations and an edge-light effect to the bottom bar
- Followed the Miuix Glass component styles and unified top bars, popups, corners, and blur
- Moved navigation to Decompose and unified the back stack and predictive back
- Added section navigation and a notification settings entry to Settings
- Added segmented tabs, paged search, and list-state components
- Regrouped network settings; Tun is grouped by interface/protocol/routing, and eBPF options merged into service options
- Updated the proxy page with single/double-column layouts and group tabs, and improved sorting, search, and delay tests
- Improved nested swiping between proxy groups and node lists
- Updated the home screen: unified on Moe, and added quote author, status-bar spacing, and phone/tablet support
- Updated Shizuku lifecycle with destroy handling
- Updated Substore with local CORS and read-only Javet; pinned the Javet native to the compiled version
- Improved the update dialog with loading progress, measured height, and no extra scrollbar
- Improved animations and rendering; unified top-bar scrolling, popups, and blur
- Updated NDK 30 and native 6.0.2 checksums
- Updated Compose, Miuix, Sketch, Camera, MMKV, boa_engine, and Go module dependencies
- Updated CI to pin Rust nightly and move to Ubuntu 26.04
- Moved the build to the Kotlin Toolchain with `kotlin` / `kotlin.bat`
- Fixed network image loading by using bundled node flags instead of remote images
- Fixed repeated app icon decoding by loading and caching in the background
- Fixed missing Super Island thumbnails by aligning the `miui.focus` image keys
- Fixed notification permission and state refresh after Shizuku authorization
- Fixed Wi-Fi automation notifications replacing the foreground service notification
- Fixed late MIUI / HyperOS app-list permission requests
- Fixed unhandled exceptions when creating an update download entry
- Fixed API protocol errors being swallowed as JVM errors
- Fixed traffic stats recording the restart gap as one large delta and filtered implausible jumps
- Fixed the config reload flag being set with a remote controller
- Fixed the YAML secret line index
- Fixed file URL validation

**Full Changelog**: [https://github.com/YumeYucca/YumeBox/compare/v0.6.5...v0.7.0](https://github.com/YumeYucca/YumeBox/compare/v0.6.5...v0.7.0)

## Choosing an APK variant

- `builtin`: Includes Geo data; recommended for first-time installation
- `external`: Smaller APK; recommended for updates
- Both variants have the same features