<div align="center">

# Melodix

**Feel Every Note**

Melodix is an open-source music player for Android with a minimal dark theme,
vibrant accents, Spotify integration and lossless playback support.

[![License](https://img.shields.io/github/license/Cosm1cBug/Melodix?style=flat-square&color=2B3137&labelColor=161B22)](https://github.com/Cosm1cBug/Melodix/blob/main/LICENSE)

</div>

---

## Features

- 🎨 Minimal dark theme with vibrant violet / cyan / magenta accents
- 🎵 Play music from YouTube Music with a fast, modern player
- 🟢 **Spotify integration** — log in with your Spotify account and play your
  playlists, liked songs, top tracks and personalized recommendations
- ▶️ **YouTube Music account sync** — log in with Google to sync your YouTube
  playlists and likes into the library, with per-playlist sync selection
- 💎 **Qobuz lossless engine** — stream AAC 320 / CD / Hi-Res lossless quality
- 🎛️ Multiple player designs (classic and modern), sliders and themes
- 📝 Lyrics support with multiple providers
- ⏲️ Sleep timer, equalizer access, Android Auto support
- 📥 Downloads and offline playback
- 🎨 Dynamic theming, custom palettes and pure-black mode
- 🔒 No ads, no tracking — your music, your way

## Design

| | |
|---|---|
| Background | `#0A0A0E` |
| Primary accent | Violet `#7C4DFF` |
| Secondary accent | Cyan `#00E5FF` |
| Highlight | Magenta `#FF2E9A` |

Logo: an "M" formed from equalizer bars.

## Installation

1. Go to the [Releases](https://github.com/Cosm1cBug/Melodix/releases) page
2. Download the latest APK
3. Install and enjoy

## Building from source

Requirements: JDK 21, Android SDK (platform 36).

### Debug (development)

```bash
git clone https://github.com/Cosm1cBug/Melodix.git
cd Melodix
chmod +x gradlew
./gradlew assembleFossDebug
```

The APK is generated in `app/build/outputs/apk/universal/debug/`.

### Release (distribution)

Release builds are signed and minified. Create a keystore once, export the
credentials the build expects, then build:

```bash
mkdir -p app/keystore
keytool -genkeypair -v -keystore app/keystore/release.keystore \
  -alias melodix -keyalg RSA -keysize 2048 -validity 10000

export STORE_PASSWORD=<store password>
export KEY_ALIAS=melodix
export KEY_PASSWORD=<key password>

./gradlew assembleUniversalRelease
```

The APK is generated in `app/build/outputs/apk/universal/release/`.

> **Warning**: never commit `app/keystore/` or your passwords. Losing the
> keystore means you can never publish an update for installed users again.

## License

This project is licensed under the **GNU General Public License v3.0** —
see the [LICENSE](LICENSE) file for details.

```
Melodix — Feel Every Note
Copyright (C) 2026 Cosm1cBug

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
```

> **Note**: Melodix is an independent project and is not affiliated,
> sponsored, or endorsed by YouTube, Google or Spotify.
