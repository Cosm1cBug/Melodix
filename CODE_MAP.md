# Melodix — Code Map & Design Guide

> "Which file handles which part of the app — and which file do I edit to
> change how something looks?"

All paths below are relative to `app/src/main/kotlin/com/melodix/music/`
unless stated otherwise (shortened as **`K/`**). Resources live in
`app/src/main/res/` (shortened **`R/`**).

---

## 1. Quick reference: "I want to change X"

| I want to change… | Edit this file |
|---|---|
| App **name** (launcher label) | `R/values/app_name.xml` (+ `R/../debug/res/values/app_name.xml` for debug builds) |
| App **icon** | `R/mipmap-*/ic_launcher*.png` + `R/mipmap-anydpi-v26/ic_launcher.xml` |
| **Brand colors** (violet/cyan/magenta, dark bg) | `K/ui/theme/Theme.kt`, `K/ui/screens/settings/PalettePickerScreen.kt`, `ThemeCreatorScreen.kt` |
| **Font / typography** | `K/ui/theme/Type.kt` + `R/font/*` |
| Any **text/string** | `R/values/strings.xml`, `melodix_strings.xml`, `spotify_strings.xml`, `qobuz_strings.xml` |
| **Now Playing screen** (whole layout) | `K/ui/player/Player.kt` |
| Player **design styles** (V1…V8, "Modern" = V2) | `K/ui/player/Player.kt` (branches on `designStyle`), names in `R/values/melodix_strings.xml` (`player_design_v1…v8`) |
| Player **background styles** (blur/gradient/glow…) | `K/ui/player/Player.kt` + `K/ui/theme/PlayerBackgroundColorUtils.kt`, `PlayerColorExtractor.kt` |
| **Seek slider** look | `K/ui/component/PlayerSlider.kt`, `K/ui/component/WavySlider.kt`, `SquigglySlider.kt` (style enum in `K/constants/PreferenceKeys.kt`) |
| **Mini player** | `K/ui/player/MiniPlayer.kt` + `MiniPlayerComponents.kt` |
| **Album art / artwork page** | `K/ui/player/Thumbnail.kt` (+ `CanvasArtworkPlayer.kt` for ambient canvas) |
| **Queue sheet** | `K/ui/player/Queue.kt` + `QueueComponents.kt` |
| **Lyrics view** | `K/ui/component/Lyrics.kt`, `LyricsV2.kt`, `K/ui/player/LyricsScreen.kt`, `K/lyrics/` |
| **Home screen** | `K/ui/screens/HomeScreen.kt` + `HomeScreenComponents.kt` + `K/viewmodels/HomeViewModel.kt` |
| **Search screens** | `K/ui/screens/search/OnlineSearchScreen.kt`, `OnlineSearchResult.kt` + `K/viewmodels/OnlineSearchViewModel.kt` |
| **Library tabs** | `K/ui/screens/library/LibraryScreen.kt` (tab host), `LibraryMixScreen.kt`, `LibraryPlaylistsScreen.kt` (contains the Spotify tile), `LibrarySongsScreen.kt`, `LibraryAlbumsScreen.kt`, `LibraryArtistsScreen.kt` |
| **Spotify screens** | `K/ui/screens/SpotifyLoginScreen.kt`, `SpotifyHomeScreen.kt`, `SpotifyLibraryScreens.kt`, `K/ui/screens/playlist/SpotifyPlaylistScreen.kt`, `K/ui/screens/settings/SpotifySettings.kt` |
| **Settings root & pages** | `K/ui/screens/settings/SettingsScreen.kt` + one file per page (see §6) |
| **Navigation / routes** | `K/ui/screens/NavigationBuilder.kt` |
| **Bottom navigation bar / scaffold** | `K/MainActivity.kt` |
| **List & grid rows** (song/artist/playlist items everywhere) | `K/ui/component/Items.kt` |
| **Settings rows/switches** | `K/ui/component/Preference.kt` |
| **Menus** (long-press bottom sheets) | `K/ui/menu/*` + `K/ui/component/BottomSheetMenu.kt` |
| **Icons** | `R/drawable/*.xml` (+ `object Icon` in `K/ui/component/Items.kt` for the in-app icon set) |
| **Playback behavior** (stream resolving, media session) | `K/playback/MusicService.kt`, `PlayerConnection.kt` |
| **Qobuz engine** | `K/qobuz/QobuzAudioProvider.kt` + `K/ui/screens/settings/QobuzSettingsScreen.kt` |
| **Spotify engine** (matching, caching) | `K/playback/SpotifyYouTubeMapper.kt`, `SpotifyMetadataRegistry.kt`, `SpotifyProfileCache.kt`, `SpotifyRecommendationEngine.kt` |
| **Spotify API client** | `spotify/` Gradle module (`spotify/src/com/melodix/music/spotify/*`) |
| **Database tables/queries** | `K/db/DatabaseDao.kt`, `K/db/entities/*`, `K/db/MusicDatabase.kt` (version + migrations) |
| **Discord status** | `K/utils/DiscordRPC.kt` + `K/ui/screens/settings/DiscordSettings.kt` |
| **Widget** | `K/widget/*` (do not rename — provider wired in `AndroidManifest.xml`) |

---

## 2. Gradle modules (top level)

| Module | Purpose |
|---|---|
| `app/` | The whole UI + playback + DB |
| `innertube/` | YouTube Music client (search, browse, stream metadata) |
| `spotify/` | Spotify web/GQL client + mapper (auth via sp_dc + TOTP) |
| `kizzy/` | Discord Rich Presence transport |
| `lrclib/`, `kugou/`, `betterlyrics/` | Lyrics providers |
| `lastfm/` | Last.fm scrobbling |
| `canvas/` | Ambient "canvas" artwork providers |
| `shazamkit/` | Music recognition |
| `jossredconnect/` | JossRed streaming fallback client |
| `simpmusic/`, `paxsenix/` | Extra source engines |

---

## 3. App shell

- **`K/App.kt`** — Application class: DI setup, theme bootstrap, notifications.
- **`K/MainActivity.kt`** — single Activity: Scaffold, bottom nav bar, NavHost
  hosting, mini-player placement, player bottom sheet, global insets
  (`LocalPlayerAwareWindowInsets`), `LocalDatabase` / `LocalPlayerConnection`
  providers, deep links. *Any global chrome change happens here.*
- **`K/ui/screens/NavigationBuilder.kt`** — every route (`"settings/appearance"`,
  `"spotify_library"`, `"player"`…) and its screen + transitions.

---

## 4. Theming & global look

- **`K/ui/theme/Theme.kt`** — builds the Material3 color scheme (dynamic color,
  pure black, palette selection). **Start here for global colors.**
- **`K/ui/theme/Type.kt`** — typography; `AppFontFamily = FontFamily(Font(R.font.linotte))`.
- **`R/font/`** — `linotte.otf` (app font), `poppins.ttf`, `anybody.ttf`, `sfprodisplaybold.ttf`.
- **`K/ui/screens/settings/PalettePickerScreen.kt` / `ThemeCreatorScreen.kt`** —
  user-facing palette creation (materialKolor).
- **`K/ui/screens/settings/AppearanceSettings.kt`** — the Appearance page: dark
  mode, player design style, slider style, thumbnails, mini-player design, etc.
- **`K/constants/Dimensions.kt`** — global sizes (`ListItemHeight`,
  `GridThumbnailHeight`, `ThumbnailCornerRadius`…). One place to resize rows/grids.
- **`K/constants/PreferenceKeys.kt`** — every settings key + style enums
  (`PlayerDesignStyle V1..V8`, `SliderStyle`, `PlayerBackgroundStyle`, …).

---

## 5. The player (Now Playing)

- **`K/ui/player/Player.kt`** — *the* now-playing screen (ported engine). Contains:
  - `useNewPlayerDesign = designStyle != PlayerDesignStyle.V1` → classic vs modern branches
  - transport buttons (play/pause/prev/next with animated weights)
  - header row, buttons row, color logic per background style
- **`K/ui/player/Thumbnail.kt`** — artwork page, snap/peek behavior, canvas toggle.
- **`K/ui/player/PlayerComponents.kt`** — helper composables used by Player.
- **`K/ui/player/MiniPlayer.kt` + `MiniPlayerComponents.kt`** — the collapsed bar.
- **`K/ui/player/Queue.kt` + `QueueComponents.kt`** — queue bottom sheet.
- **`K/ui/player/LyricsScreen.kt`** + **`K/ui/component/Lyrics*.kt`** + **`K/lyrics/`** — lyrics.
- **`K/ui/player/PlaybackError*.kt`** — error card + recovery actions.
- Slider visuals: **`K/ui/component/PlayerSlider.kt`**, **`WavySlider.kt`**, **`SquigglySlider.kt`**.
- Color extraction from artwork: **`K/ui/theme/PlayerColorExtractor.kt`**,
  `PlayerBackgroundColorUtils.kt`, `PlayerSliderColors.kt`.

---

## 6. Settings pages (`K/ui/screens/settings/`)

`SettingsScreen.kt` is the root. Each page:

`AppearanceSettings` (looks), `PlayerSettings`, `ContentSettings`,
`AccountSettings`, `IntegrationScreen` (entry list → Spotify/Qobuz/Discord/LastFM),
`SpotifySettings`, `QobuzSettingsScreen`, `DiscordSettings` (+`DiscordExperimental`),
`LastFMSettings`, `AndroidAutoSettings`, `AlwaysOnDisplaySettings`,
`BackupAndRestore`, `StorageSettings`, `PrivacySettings`, `DebugSettings`,
`AboutScreen` (branding/links), `UpdateScreen`, `MusicTogetherScreen`.

Support files: `SettingsLayout.kt`, `SettingsComponents.kt`,
`SettingsDataBuilders.kt`, `SettingsModels.kt`, `SettingsDimensions.kt`,
`SettingsQuickActions.kt`, `SettingsSearch.kt`.

---

## 7. Main screens

- **Home:** `ui/screens/HomeScreen.kt`, `HomeScreenComponents.kt`,
  `viewmodels/HomeViewModel.kt`.
- **Search:** `ui/screens/search/*`, `viewmodels/OnlineSearchViewModel.kt`,
  `OnlineSearchSuggestionViewModel.kt`, `LocalSearchViewModel.kt`.
- **Library:** `ui/screens/library/*` (see §1). The **Spotify tile** lives in
  `LibraryPlaylistsScreen.kt` (`PlaylistShortcutEntry` with route `spotify_library`);
  the folder view is `SpotifyLibraryScreen` in `ui/screens/SpotifyLibraryScreens.kt`.
- **Detail screens:** `AlbumScreen.kt`, `artist/`, `playlist/`, `StatsScreen.kt`,
  `HistoryScreen.kt`, `ChartsScreen.kt`, `ExploreScreen.kt`, `NewReleaseScreen.kt`,
  `YearInMusicScreen.kt`, `BrowseScreen.kt`.

---

## 8. Shared components (`K/ui/component/`)

- **`Items.kt`** — `YouTubeListItem`, `YouTubeGridItem`, `PlaylistListItem`,
  `object Icon` (in-app icon composables). *Change row/grid look here.*
- **`Preference.kt`** — `PreferenceEntry`, `SwitchPreference`, `EnumListPreference`.
- **`BottomSheet.kt` / `BottomSheetMenu.kt` / `BottomSheetPage.kt`** — sheet chrome.
- **`NavigationTitle.kt` / `NavigationTile.kt`** — section headers/tiles.
- **`ChipsRow.kt`** — filter chips; **`HideOnScrollFAB.kt`** — floating buttons.
- **`Library.kt`** — library feature cards/tiles.
- Dialogs: `Dialog.kt`, `CreatePlaylistDialog.kt`, `TextFieldDialog`(s), etc.

---

## 9. Playback & data

- **`K/playback/MusicService.kt`** — Media3 service, stream resolution chain
  (YouTube → Qobuz → JossRed), media session, downloads, Spotify hooks.
- **`K/playback/PlayerConnection.kt`** — the state bridge the UI collects from.
- **`K/playback/queues/`** — queue implementations (`SpotifyQueue`,
  `SpotifyPlaylistQueue`, `SpotifyLikedSongsQueue`, `YouTube` queues…).
- **`K/db/`** — Room DB. Schema change = edit entity + bump
  `CURRENT_VERSION` in `MusicDatabase.kt` (auto-reconcile migration).
- **`K/viewmodels/`** — one VM per screen family.

---

## 10. Resources (`app/src/main/res/`)

| Folder | What |
|---|---|
| `values/strings.xml` | core strings |
| `values/melodix_strings.xml` | Melodix-specific strings (player design names, etc.) |
| `values/spotify_strings.xml` / `qobuz_strings.xml` | integration strings |
| `values/app_name.xml` | app name |
| `values/colors.xml`, `styles.xml` | base colors & themes |
| `drawable/` | all vector icons (`spotify.xml`, `pause.xml`, `play.xml`, …) |
| `font/` | fonts |
| `mipmap-*/` | launcher icons |
| `xml/` | widget layouts, shortcuts, preferences XML |

AndroidManifest: `app/src/main/AndroidManifest.xml` (permissions, widget
provider, deep-link intent filters, theme).

---

## 11. Tips for safe visual edits

1. **Prefer theme-level changes** (`Theme.kt`, `Dimensions.kt`) over editing
   individual screens — everything inherits.
2. Player look changes: search `Player.kt` for `useNewPlayerDesign` (modern)
   vs the `else` branch (classic) and edit the branch you see on device.
3. Strings/icons never require Kotlin changes — only `res/`.
4. After any change: `./gradlew assembleFossDebug` in a **fresh folder**.
5. Keep this file updated when moving/renaming screens.
