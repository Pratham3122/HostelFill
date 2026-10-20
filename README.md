# HostelFill
**By Pratham Shah**  
*Your Hostel Forms, Filled Smarter.*

---

## What it does

HostelFill is an Android app that fills your recurring hostel Google Form automatically.

1. Enter your details once (name, email, phone, room, meal preferences).
2. Every day: copy the form link from WhatsApp → open HostelFill → paste → tap **Start Autofill**.
3. The form opens inside the app and is filled using your saved profile.
4. Review and submit, or enable **Auto-Submit** for fully hands-free operation.

---

## Architecture

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Local storage | Android DataStore (Preferences) |
| Form interaction | WebView + JavaScript injection |
| No backend | All data stays on device |

---

## Form Fields Supported

The app is pre-configured for these hostel form questions:

| Form Question | Mapped to |
|---------------|-----------|
| EMAIL / Email Address | Saved email |
| PHONE NUMBER / Mobile Number / Contact Number | Saved phone |
| ROOM NUMBER / Room No. | Saved room number |
| FULL NAME / Student Name / Name | Saved full name |
| BREAKFAST | Saved breakfast choice (0 or 1) |
| LUNCH | Saved lunch choice (0 or 1) |
| DINNER | Saved dinner choice (0 or 1) |

Unknown questions are flagged and you can map them manually inside the app.

---

## Google Account / Authentication

- If the form requires Google sign-in, the app shows a banner and pauses.
- Sign in **normally** inside the embedded browser — no password is captured.
- Once signed in and the form loads, tap **Start Autofill** again.
- If the form auto-collects your Google email, that field is skipped (not overridden).

---

## Building the APK

### Requirements

- Android Studio Hedgehog (2023.1.1) or later **or** JDK 17 + Android SDK 35
- Android SDK Build Tools 35.0.0
- Gradle 8.7 (downloaded automatically by the wrapper)

### Steps

```bash
# 1. Open in Android Studio
#    File → Open → select the HostelFill/ folder

# 2. Let Gradle sync finish

# 3. Build debug APK
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Install on your phone

**Method A — USB**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Method B — File transfer**
1. Copy `app-debug.apk` to your phone.
2. Open the file manager on your phone.
3. Tap the APK → install.
4. Enable "Install from unknown sources" if prompted (Settings → Security).

**Method C — GitHub Actions (build without a laptop)**
1. Push this folder to a GitHub repository.
2. Create `.github/workflows/build.yml`:
```yaml
name: Build APK
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```
3. Download the APK from the Actions tab after the build completes.

---

## Running Unit Tests

```bash
./gradlew :app:test
```

Tests cover: URL validation, label normalization, all 7 field mappings, profile validation, autofill script generation.

---

## Implemented Features

- [x] First-time profile setup (7 fields)
- [x] Profile editing and data deletion
- [x] Profile data persists across restarts (DataStore)
- [x] Google Forms URL validation (forms.gle + docs.google.com/forms)
- [x] URL sharing via Android Sharesheet (from WhatsApp or any app)
- [x] Form opens inside the app (WebView)
- [x] JavaScript injection fills text fields and radio buttons
- [x] Google sign-in detection and user guidance
- [x] Email auto-collection detection (skips override)
- [x] Per-submission meal override (without changing saved defaults)
- [x] Configurable meal option labels (0/1 meaning set by user)
- [x] Unknown question detection and manual mapping
- [x] Custom mappings saved and reused
- [x] Review Mode: fill then let user review before submitting
- [x] Auto-Submit Mode: submit after all fields confirmed filled
- [x] Autofill result report (filled / skipped / unknown / errors)
- [x] Unit tests for all core logic
- [x] No backend, no internet data transmission, no paid API

---

## Known Limitations

| Limitation | Reason |
|-----------|--------|
| Form DOM may change | Google updates Forms HTML; JS selectors may need updating |
| CAPTCHA not automatable | By design — user must solve manually |
| File upload fields not supported | WebView JS cannot upload files |
| Submission confirmation may not always be detected | Depends on Google's redirect behavior |
| forms.gle short URLs require network to resolve | Redirect followed by the WebView automatically |
| Duplicate submission prevention is best-effort | App tracks `__SUBMITTED__` flag in JS; network errors may still cause ambiguity |

---

## Privacy

- All data stored locally using Android DataStore.
- No analytics, no ads, no external server calls.
- Profile data never leaves the device.
- Cookies are used only for Google sign-in inside the WebView (standard browser behavior).

---

*HostelFill — By Pratham Shah*
