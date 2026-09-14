# 🐍 Venom Launcher

A home screen replacement for Android, written in Kotlin + Jetpack Compose.
Dark, glassy, fast — and it ships with a charging engine nobody else has.

```
minSdk 26 (Android 8.0)  ·  targetSdk 37 (Android 17)  ·  single module, no DI framework
```

---

## What it does

### Core
| | |
|---|---|
| **Home** | Paged icon grid, drag to move, drop one icon on another to make a folder |
| **Widgets** | Real `AppWidgetHost` hosting, multi-cell spans, resize, remove, full picker |
| **Dock** | Glass dock, configurable slot count, long-press for options |
| **Drawer** | Search with prefix ranking, alphabet scrubber, sort modes, hidden apps |
| **Icons** | Third-party icon packs (Nova/ADW/GO/Atom formats), uniform masks incl. true squircle |
| **Look** | 6 accents + Android 12+ dynamic colour, wallpaper blur/dim, icon scale, label control |
| **Gestures** | Swipe up / down / left / right / double-tap all remappable |

### ⚡ Charge Lab — the part no other launcher ships
- **Charging animation**: a Canvas-drawn liquid battery that fills to the real level with a travelling wave, rising bubbles and a breathing glow
- **Live telemetry**: volts, milliamps, watts, charge-speed class (Trickle → Hyper), temperature, battery health, chemistry, estimated capacity in mAh
- **Time to full / to empty**, computed from the charge counter and instantaneous current
- **Battery caretaker**: set a charge limit (40–100%) and Venom notifies you when you hit it, so you can unplug before the stressful top end
- **Heat guard**: warns when the cell passes 42 °C while charging
- **Charging curve**: samples every 20 s while plugged in and draws the % curve
- **Session history**: start %, gained %, duration, peak watts, peak temperature, plug type

### 🧠 "Pro" stuff
- **Prediction row** — builds a per-app hourly histogram from `UsageEvents` and re-ranks suggested apps by time of day. WhatsApp in the morning, YouTube at midnight.
- **App shortcuts** — long-press → real `ShortcutManager` deep links (dynamic + manifest + pinned)
- **Notification dots** — counts via a `NotificationListenerService`; nothing is read, stored or transmitted
- **App stats** — 7-day screen time per app
- **Backup / restore** the whole home layout to a JSON file
- **Squircle masking** — a real superellipse path, not a rounded rectangle

---

## Build it

```bash
git clone https://github.com/MykelGoal/Venom-launcher-.git
cd Venom-launcher-
./gradlew :app:assembleDebug      # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug       # straight to a connected device
```

Or open the folder in **Android Studio** (Giraffe or newer; it bundles the right JDK).

Requirements: JDK 17, Android SDK with **platform 37 + build-tools 37**.
Toolchain versions live in `gradle/libs.versions.toml`-style blocks at the top of
`build.gradle.kts` and `app/build.gradle.kts`:

| | version |
|---|---|
| Android Gradle Plugin | 9.4.0 |
| Kotlin | 2.4.20 |
| Compose BOM | 2026.09.00 |
| Gradle | 9.7.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |

---

## Install & set as default

1. `adb install app-debug.apk` (or open the APK on the phone)
2. Press **Home** → pick **Venom** → *Always*
3. If your OEM hides the chooser: **Venom Settings → Set Venom as default launcher**

To get back: Settings → Apps → Default apps → Home app.

---

## Permissions — what they're for

| Permission | Why | Required? |
|---|---|---|
| `QUERY_ALL_PACKAGES` | Enumerate installed apps for the drawer. Drop it if you publish to Play and rely on the `<queries>` block instead. | yes |
| `PACKAGE_USAGE_STATS` | Prediction row + app stats. **Off until you grant it** in Settings → Usage access. | optional |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Notification dots. **Off until you grant it** in Notification access. | optional |
| `BIND_DEVICE_ADMIN` | Double-tap to lock only. **Off until you activate it.** | optional |
| `POST_NOTIFICATIONS` | Charge-limit alert on Android 13+. | optional |
| `REQUEST_DELETE_PACKAGES` | Uninstall shortcut in the app menu. | optional |

Venom has no network code, no analytics, and no ads. Grep for `Http`, `OkHttp` or
`Firebase` — there is nothing there.

---

## Layout

```
app/src/main/kotlin/com/venom/launcher/
├── MainActivity.kt            single activity, edge-to-edge, HOME intent
├── VenomApp.kt                starts ChargeRepository, keeps the widget host warm
├── data/
│   ├── AppInfo / AppRepository        installed apps
│   ├── LauncherItem / HomeLayout      serialisable home screen (JSON in DataStore)
│   ├── LauncherSettings / Prefs       every knob, persisted
│   ├── IconPack / IconLoader          icon packs + uniform masks + LRU
│   ├── UsageRepository                usage stats + hourly affinity histogram
│   ├── ChargeRepository               battery telemetry engine
│   ├── ChargeModels / ChargePrefs     state, sessions, samples, limit
│   └── VenomBus / VenomWidgetHost     tiny event bus, singleton AppWidgetHost
├── vm/LauncherViewModel.kt    all layout mutation + predictions + widget binding
├── ui/
│   ├── LauncherRoot.kt        state machine, overlays, gesture dispatch
│   ├── screens/               Home, Drawer, Settings, ChargeLab
│   └── components/            grid, dock, icons, wallpaper, charge animation, sheets
├── receiver/                  package installs, device admin
├── service/                   notification listener (dot counts)
└── util/SystemActions.kt      launch, uninstall, status bar, lock, role manager
```

State lives in **DataStore** — settings as typed preferences, the home layout as a
single JSON blob. There is no Room, no DI container, no repository abstraction
layer you have to fight.

---

## Known rough edges (v0.1.0)

- Swipe-down for notifications uses reflection on the hidden `StatusBarManager`.
  Most devices allow it; some Android 12+ builds block it, in which case Venom
  falls back to opening the drawer.
- Icon packs that only ship `iconback`/`iconupon` (no `appfilter.xml` entries)
  fall back to system icons — full mask composition isn't implemented yet.
- Widget resizing is span-based (snap to cells), not free-form pixel resizing.
- Gestures are bound to: swipe up/down on the grid, swipe left/right on the
  clock strip, double-tap anywhere.

---

## Roadmap

- [ ] Icon-pack masking fallback (`iconback` / `iconmask` / `iconupon`)
- [ ] Free-form widget resize handles
- [ ] Icon pack calendar + clock icon support
- [ ] Automated layout backup on change
- [ ] Per-app icon/label overrides via long-press → Edit
- [ ] Material You colour extraction from the wallpaper without dynamic colour

---

## License

MIT — see [LICENSE](LICENSE).
