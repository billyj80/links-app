# Links — Self-Hosted Services Manager

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![F-Droid](https://img.shields.io/f-droid/v/com.home.servicelinks)](https://f-droid.org/packages/com.home.servicelinks/)

A free, open-source Android app for organising access to your self-hosted home-server services — all in one place, with no tracking, no accounts and no internet requirement.

---

## Features

- **Links tab** — add services with Tailscale, Cloudflare Tunnel, LAN and custom URL endpoints
- **Apps tab** — launch any installed app as a shortcut with one tap
- **Colour-coded pills** — distinct colours for Tailscale, Cloudflare and LAN links
- **Compact / expanded** card layout toggle
- **Dark / Light / System** theme
- **Backup & Restore** — JSON format and legacy `Service Links Backup (.txt)` compatible
- **Reorder** services and app shortcuts
- **Custom icons** — choose from preset icon library or pick from gallery
- **English & Polish** language support
- **No analytics, no ads, no trackers**

---

## Build from source

### Requirements
- Android Studio Hedgehog or newer  
- JDK 17  
- Android SDK Platform 35

### Steps
```bash
git clone https://github.com/billyj80/links-app.git
cd links-app
./gradlew assembleRelease
```

The unsigned APK will be at `app/build/outputs/apk/release/app-release-unsigned.apk`.

---

## F-Droid

This app is designed to be built and distributed via [F-Droid](https://f-droid.org/).

- No proprietary SDKs or non-free libraries  
- No Google Play Services dependency  
- All dependencies resolved from Maven Central / Google Maven  
- `dependenciesInfo` disabled for reproducible builds  
- Proguard/R8 enabled for release builds  

Metadata is in [`fastlane/metadata/android/`](fastlane/metadata/android/).

---

## Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Open service URLs in the browser |
| `QUERY_ALL_PACKAGES` (via `<queries>`) | List installed apps for the App Shortcuts tab |

---

## License

[GNU General Public License v3.0](LICENSE)
