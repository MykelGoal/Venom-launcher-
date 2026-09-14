# Venom Launcher — Roadmap to "best launcher ever"

45 Kotlin files today. The list below is the gap between *good* and *top of the pro list*.
`[x]` = shipped · `[ ]` = next · `[ ]` with a batch number = already scheduled.

## Shipped

**v0.1 — core**
- [x] Home grid with pages, dock, drag & drop
- [x] App drawer (grid + sort modes)
- [x] Squircle / hexagon / circle icon masking (true superellipse path)
- [x] Icon pack support (Nova / ADW / GO / Atom `appfilter.xml`)
- [x] Drag-to-folder + folder dialog
- [x] Real `AppWidgetHost` (add/remove widgets)
- [x] Notification dots (counts only, privacy-safe)
- [x] Long-press app shortcuts
- [x] Per-gesture config (swipe u/d/l/r, double tap, home press)
- [x] Prediction row (per-app 24-hour usage histogram)
- [x] 7-day screen time
- [x] Material You dynamic colour (Android 12+)
- [x] Wallpaper dim + blur
- [x] JSON layout export / import
- [x] Hidden apps

**v0.1 — Venom Charge Lab**
- [x] Liquid-fill battery animation (wave, bubbles, breathing glow, shimmer)
- [x] Live volts / mA / watts / temp / health
- [x] Charge-speed class IDLE → HYPER
- [x] Minutes-to-full estimate
- [x] Charge-limit alert (Battery Caretaker)
- [x] Heat guard (42 °C)
- [x] Charging-curve sparkline + session history
- [x] Full-screen plug-in overlay

**v0.2 — Game Mode**
- [x] Auto game detection (UsageStats + `CATEGORY_GAME`)
- [x] FPS / temp / battery HUD drawn over the game
- [x] Frame-time sparkline (stutter you can see)
- [x] Session recording (playtime, drain, peak temp, avg/min FPS)
- [x] Notification snoozing while gaming
- [x] Game Library with lifetime per-game stats

## Batch 3 — Game Turbo  *(in progress)*
- [ ] Floating draggable Turbo bubble → expands into a control panel
- [ ] Boost: clear cached background processes when a game opens
- [ ] Do Not Disturb while gaming (real zen mode, restored after)
- [ ] Brightness lock per game (restored after)
- [ ] Mute / unmute from the bubble
- [ ] Thermal guard alert (sustained 43 °C)
- [ ] Post-session summary notification
- [ ] Per-game profiles (each game remembers its own boost / DND / brightness)

## Batch 4 — Find anything instantly
- [ ] Universal search bar: apps, contacts, settings, web, calculator
- [ ] T9 / fuzzy matching, ranked by your own usage
- [ ] A–Z fast-scroller down the side of the drawer
- [ ] Category tabs in the drawer (Games / Social / Tools / Recent)
- [ ] Search actions: uninstall, app info, add to home, shortcuts

## Batch 5 — Notifications done properly
- [ ] Real notification shade (own UI, not the reflection hack)
- [ ] Reply inline from the shade
- [ ] Heads-up popups
- [ ] Notification grouping + snooze + "clear all"
- [ ] Media / now-playing card with transport controls (MediaSession)

## Batch 6 — Looks & feel
- [ ] Icon-to-app open animation (container transform)
- [ ] Spring physics overscroll
- [ ] Page transition effects (cube / carousel / fade / stack)
- [ ] Real blur behind dock, folders and sheets (RenderEffect, 12+)
- [ ] Themed icons (monochrome Material You from the icon pack)
- [ ] Clock/date header styles + custom fonts
- [ ] Parallax wallpaper across pages
- [ ] At a Glance: date, next calendar event, weather-ready slot

## Batch 7 — Folders & organisation
- [ ] Auto-categorised smart folders (Games / Social / Tools / Media)
- [ ] Folder cover icon + tint + custom name
- [ ] Folder blur/opacity, grid size per folder
- [ ] Batch select: drag many icons into a folder at once
- [ ] Desktop lock (no accidental edits)

## Batch 8 — Privacy & control
- [ ] App lock with PIN (any app, not just hidden ones)
- [ ] Hidden apps behind PIN + fake "not found" in search
- [ ] Private folder on the home screen
- [ ] Per-app usage limits with a nudge when you go over
- [ ] Screen-time widget

## Batch 9 — Power user
- [ ] Full backup & restore (layout + settings + profiles) to one file
- [ ] Widget resize + preview picker
- [ ] Swipe-up gesture on an icon = custom action
- [ ] Dock: page indicator styles, extra shortcuts
- [ ] Two-finger gestures (pinch = overview, rotate = wallpaper)
- [ ] Multi-language (starting with your languages)
- [ ] Custom accent picker + per-screen wallpaper

## Batch 10 — Charge Lab 2
- [ ] Charge alarm (full / unplug reminder)
- [ ] Low-battery reminder with custom thresholds
- [ ] Battery health estimate + charge-cycle counter
- [ ] Charge history graphs over weeks
- [ ] Fast-charge detection per charger/cable

---

Batches land one at a time, each one compiled, committed, pushed and released as a new APK at
<https://github.com/MykelGoal/Venom-launcher-/releases>.
