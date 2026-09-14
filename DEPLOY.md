# Venom Launcher — Deploy & Install Guide

How to get the APK onto your phone and actually start using it as your home screen.

---

## 0. What you need

| Thing | Notes |
|---|---|
| The APK | `release/venom-launcher-debug.apk` (13 MB, minSdk 26 = Android 8.0+) |
| An Android phone | Android 8.0 or newer. Best experience on Android 12+ |
| USB cable **or** any file-sharing app | To move the APK to the phone |
| `adb` (optional, faster) | Part of `platform-tools`. Lets you install over Wi-Fi/USB in one command |

---

## 1. Install the APK

### Option A — copy the file to the phone (no PC tools needed)

1. Send `venom-launcher-debug.apk` to your phone: USB file transfer, WhatsApp/Telegram to yourself, Google Drive, Bluetooth — anything.
2. On the phone, open the file from your **Files / Downloads** app.
3. Android will say *"Install unknown apps"* → tap **Settings** → allow **"From this source"** (one-time per source app).
4. Back out and tap **Install**.

> If Android blocks it on Android 8+: **Settings → Apps → Special access → Install unknown apps** → pick the app you opened it from → **Allow**.

### Option B — adb (recommended if you're a dev)

```bash
# USB, phone plugged in with USB debugging on
adb install -r release/venom-launcher-debug.apk

# or over Wi-Fi (Android 11+): Wireless debugging → pair, then
adb connect <phone-ip>:5555
adb install -r release/venom-launcher-debug.apk
```

`-r` = reinstall keeping data. Add `-g` to grant all runtime permissions at install time.

---

## 2. Make it your home screen (the important bit)

An app only becomes "the launcher" when Android asks you to pick it.

1. Press **Home**.
2. If a picker appears → choose **Venom Launcher** → tap **Always**. ✅ Done.
3. If it *doesn't* appear (common on Samsung/Xiaomi/OnePlus), do it manually:
   - **Settings → Apps → Default apps → Home app** (or *Default home app*) → **Venom Launcher**.
   - Samsung One UI: **Settings → Apps → Choose default apps → Home app**.
   - Xiaomi MIUI/HyperOS: **Settings → Apps → Manage apps → ⋮ → Default apps → Home**.

> **Tip:** before switching, open Venom Launcher **once** from the app drawer so it finishes first-run setup. Also know how to get back: Settings → Apps → Venom Launcher → *Open by default / Home app* → clear, or just uninstall if something goes wrong.

---

## 3. Grant the permissions that unlock the features

Venom is a *real* launcher, so its best features need real permissions. The app asks for these the first time you open each screen — you can also grant them directly:

| Permission | Where | Unlocks |
|---|---|---|
| **Usage access** | Settings → Apps → Special access → Usage access → Venom | Game detection, prediction row, 7-day screen time |
| **Notification access** | Settings → Notifications → Notification access (or *Device & app notifications*) → Venom | Notification dots, notification snoozing while gaming |
| **Draw over other apps** (`SYSTEM_ALERT_WINDOW`) | Settings → Apps → Special access → Display over other apps → Venom | In-game HUD (FPS/thermal) + the full-screen charge animation |
| **Device admin** (optional) | Settings → Security → Device admin apps → Venom | Double-tap/lock gesture to sleep the screen |

In-app shortcuts: **Venom Settings → Permissions** section → each row opens the exact system page.

Rough edges to expect:
- The **swipe-down notification shade** gesture uses a hidden Android API via reflection — it works on most stock/AOSP ROMs but can silently do nothing on heavily skinned ones.
- **Icon packs** that only ship `iconback`/`iconupon` (no per-app drawables) fall back to your system icons.

---

## 4. Turn on Game Mode

1. Venom Settings → **Gaming** → toggle **Game Mode**.
2. Grant **Draw over other apps** (needed for the HUD).
3. Leave **Auto-detect games** on — it uses `UsageStatsManager` + the app's `CATEGORY_GAME` flag, no manual list.
4. Launch any game → a small HUD appears top-left:
   - **FPS** (green ≥ 50, amber 30–49, red < 30)
   - frame-time sparkline (stutter you can *see*)
   - battery % and temperature
5. Optional toggles: which HUD fields to show, and **block notifications while gaming** (they're snoozed 3 h, not deleted — they come back when you're done).
6. **Game Library** (home → gesture, or Settings → Gaming → Game Library) shows every game with lifetime playtime, average FPS, battery drain and peak temp per title.

---

## 5. Building it yourself

Toolchain: **JDK 17**, Android SDK **platform android-37.1**, **build-tools 37.0.0**, Gradle **9.7.0**, AGP **9.4.0**, Kotlin **2.4.20**, compileSdk/targetSdk **37**, minSdk **26**.

```bash
export JAVA_HOME=$HOME/.cache/tc/jdk17
export ANDROID_HOME=$HOME/.cache/tc/android-sdk
export GRADLE_USER_HOME=$HOME/.cache/gradle-home
export PATH=$JAVA_HOME/bin:$PATH

cd VenomLauncher
./gradlew :app:assembleDebug     # debug APK  → app/build/outputs/apk/debug/
./gradlew :app:assembleRelease   # release    (needs your own signing config first)
```

Before publishing a release build, add a `signingConfig` in `app/build.gradle.kts` — an unsigned/signed-with-debug-key APK can't be upgraded cleanly later.

---

## 6. Sharing it with other people

Send them the same APK and this file. Everything in sections 1–3 applies. Warn them: it's a **debug** build (no Play Store auto-updates, debug key signature), and the rough edges in section 3 are known.
