# Just Player Morveus

Personal fork of [Just Player+](https://github.com/wasky/just-player-plus) (itself a modified [Just Player](https://github.com/moneytoo/Player)), tweaked for my own setup.

## Download

Grab the latest APK from the [Releases page](https://github.com/Morveus/just-player-plus/releases). The universal APK works on Android TV, phones and tablets, and installs alongside the original Just Player+ without any conflict.

## Changes compared to Just Player+

* Renamed to **Just Player Morveus** with application id `com.morveus.player`, so it installs alongside the original Just Player+ without any conflict
* Subtitle delay adjusts in **100 ms** steps instead of 200 ms
* **Fixed subtitle delay for embedded subtitles**: the delay is now applied at render time instead of parse time. Previously, negative delays could not move embedded subtitles (MKV) earlier: they only shortened the on-screen duration, and past roughly -2 s subtitles disappeared entirely. It now works in both directions, for embedded, external and image-based (PGS/VobSub) subtitles
* Positive delay values are shown with a `+` prefix in the OSD
* **Subtitle speed correction for frame-rate mismatches**: subtitles timestamped for a different frame rate than the video (e.g. 23.976 fps subtitles on a 25 fps encode) drift more and more as playback advances, and no constant delay can fix that. Two new entries in the subtitle OSD solve it:
  * `Speed (fps mismatch)`: presets for the common conversions (23.976 ↔ 25, 24 ↔ 25, 23.976 ↔ 24)
  * `Smart resync`: sync a line near the beginning with the regular delay setting and tap it once, then when the drift shows up later, fix the delay again and tap it a second time. The player computes the exact speed factor and delay from your two sync points, snapping to the nearest standard frame-rate ratio. Delay and speed are remembered per file, like the delay already was

Subtitle delay is reachable by long-pressing the subtitle icon on the playback screen. Right arrow (+) shows subtitles later, left arrow (-) shows them earlier, like in Kodi.

## Features inherited from Just Player+

### Additional subtitle settings on the playback screen

* Experimental: Subtitle delay/advance
* Experimental: Support for MicroDVD and MPL2 subtitles
* Subtitle settings (size, style, position) can be configured by long-pressing the subtitle icon on the playback screen
* New `Outline & shadow` subtitle edge style
* New `Medium` font style in addition to Regular and Bold (requires Android 9+)
* On Android TV, you can achieve a Netflix / Nova Player–like subtitle style by setting:
  * Position: `+3`
  * Size: `-8`
  * Edge type: `Outline & shadow`
  * Typeface: `Medium`
* Custom fonts for subtitles

### Other changes

* Fix subtitle encoding when used as an external player with Nova Video Player
* The Back button hides media controls instead of quitting the app on Android TV

## How to build

```
./gradlew :app:assembleLatestUniversalRelease
```

The APK ends up in `app/build/outputs/apk/latestUniversal/release/just_player_morveus.apk`.
