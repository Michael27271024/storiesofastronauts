# Astronaut Stories (داستان‌های فضانوردها) — 100% Native Kotlin & Landscape Edition

A 100% offline, native Kotlin & Jetpack Compose 2D sci-fi action-adventure platformer locked strictly in horizontal **Landscape** orientation and styled after the official Astronaut Stories emblem (Void Black, Obsidian, Copper-Amber Gold, and Warm Cream).

## Key Features
- **Strict Landscape-Only Lock**: Locked to horizontal `landscape` mode in `AndroidManifest.xml`, `MainActivity`, and the root Compose viewport (`BoxWithConstraints` 16:9 landscape guard) so the game never rotates or stretches into a vertical portrait layout.
- **100% Native 60 FPS Game Engine**: Horizontal side-scrolling Jetpack Compose `Canvas` renderer + fixed-timestep physics (`GameSimulation` & `GameCanvasRenderer`) with planetary gravity, 3 AI squad teammates, 8 enemy types (Zombies, Alien Soldiers, Flyers, Splitters, Mini Splitters, Alien Ships, Laser Turrets, Multi-Phase Bosses), dynamic helmet-beam lighting on Mercury/Citadel, and branching Story choices.
- **100 Story Stages & 4 Offline Game Modes**:
  - **Story Mode**: 10 Planetary Chapters × 10 Stages (Moon through Return to Earth).
  - **Endless Survival**: Progressive waves & periodic bosses.
  - **Timed Blitz Challenge**: 90-second high-speed blitz.
  - **AI Squad Duel**: Offline competitive duel against an AI rival.
- **100% Offline Local Persistence & RPG Progression**:
  - Full local storage via **Room Database** and **Jetpack DataStore** (zero accounts, zero cloud login, zero internet permissions).
  - 8 Heroes (4 original + 4 unlockable), 5 Weapons, 3 Mission Boosters, 6 Skill Tree upgrades, 16 Achievements with system Share Intent, and Career Analytics.
- **Media, Accessibility & Hardware Support**:
  - **Media3 ExoPlayer** for bundled `.mp4` planetary cutscenes + native Compose Canvas cinematic trailer & interactive tutorials.
  - **SoundPool + MediaPlayer** audio engine across 15 music themes & 18 SFX.
  - **Persian (RTL), English (LTR), Arabic (RTL)** localization, Dark/Light themes, High-Contrast Accessibility mode, Left-Handed controls, WorkManager daily local mission reminders, and Bluetooth/USB Gamepad + Keyboard controls.

## Build & Run
```bash
gradle :app:assembleDebug
```
