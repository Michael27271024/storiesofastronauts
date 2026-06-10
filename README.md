# Astronaut Stories — Project Overview

این پروژه یک بازی/وب‌اپ محلی است که از یک WebView برای نمایش صفحات HTML و ویدیوهای داخل `app/src/main/assets` استفاده می‌کند.

Summary (English):
- Android app that hosts local HTML/JS/CSS assets in a WebView. The assets contain the game UI and media.

Quick start
1. Install Android SDK and Android Studio (recommended) with a matching SDK for compileSdk 36.
2. From project root (Windows PowerShell):

```powershell
.\gradlew.bat assembleDebug
```

Notes & improvements applied in this review:
- `MainActivity.kt` refactored to use lifecycle-aware coroutines for delayed navigation and safer WebSettings calls.
- Removed unneeded sensitive permissions from `AndroidManifest.xml` (phone state and location). Re-add them only if features require them.
- Added `.editorconfig` and a short README. Consider adding Spotless/ktlint/detekt and a CI workflow next.

Next recommended steps
- Add `ktlint` / `detekt` and configure Spotless for consistent formatting.
- Add unit tests for any non-UI logic and a GitHub Actions CI that runs lint/build/tests on PRs.
- Review large assets in `app/src/main/assets` for size (optimize PNGs and compress video files if distribution size matters).

If you want, I can continue and:
- add formatting and lint tooling (ktlint, detekt),
- add a GitHub Actions workflow,
- and migrate Gradle / dependencies to the latest safe versions.

