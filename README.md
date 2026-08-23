# English Bondhu

A Bengali-first, multilingual-ready English learning app built with Kotlin and Jetpack Compose.

## Current prototype

- Bengali-to-English Pre-A1→A1 path with 72 runnable lessons across 12 practical units
- Multiple-choice, listening, speaking, and translation work in every curriculum lesson
- Slow, beginner-friendly Android text-to-speech playback and speech-recognition practice
- Interactive scoring, saved accuracy, weak-item history, and lesson progress
- Persistent SM-2 review queue built from the learner's real answers
- 12-question placement test with a saved level and recommended starting lesson
- Four guided dialogue scenarios: shopping, doctor, interview, and phone call
- Direct Bengali-first Gemini 3.1 Flash-Lite tutor with offline fallback and spoken-input correction
- Direct Gemini 3.1 Flash Live audio-to-audio conversation on the phone with setup timeout and speaker-echo suppression
- Persistent completed lessons, XP, daily streak, attempts, accuracy, and placement result
- Material 3 light/dark UI and five primary areas: Home, Learn, Practice, AI Bondhu, and Live
- First-launch choice screen, lesson/topic search, incomplete filter, safe-exit confirmation, retry controls, and end-of-session summaries
- Listening prompts conceal the transcript until the learner answers; AI chat supports quick starters, keyboard Send, and automatic scrolling

Every lesson is freely accessible. Placement and the star marker recommend a useful starting point, but learners can open a later unit, skip familiar material, return to any lesson, and continue from the last lesson they chose.

The 72-lesson curriculum is CEFR-informed and internally language-checked, but it is not labeled as human teacher-approved yet. `ContentReviewStatus` and [CURRICULUM_REVIEW.md](CURRICULUM_REVIEW.md) provide an auditable sign-off path for a qualified Bengali/English teacher. Publication still requires that review plus learner piloting.

## Run locally

1. Install the latest stable Android Studio and its Android SDK 36 platform.
2. Open this folder in Android Studio.
3. Select the `app` run configuration and an Android 8+ emulator/device.
4. Sync Gradle, then run the app.

From a terminal with JDK 17 and Android SDK configured:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Microphone permission is requested only when the learner starts a speaking exercise. Speech recognition availability depends on the recognition service installed on the device.

## Architecture direction

The prototype has a small UI/data/domain separation. The production version should grow into modules without changing the content model:

```text
Compose UI -> ViewModels/UDF -> learning domain -> repositories
                                          |-> local Room progress/content
                                          |-> speech engine (system or sherpa-onnx)
                                          |-> secure AI gateway
                                          |-> content sync API
```

Source language (`bn`, later `hi`, `ta`, etc.), target language (`en`), level, lesson instructions, and exercises are data. Bengali text is not used as a routing key.

## Personal standalone AI build

For the current personal-use phase, `build-standalone-apk.ps1` decrypts the configured NovelForge Gemini key in memory and injects it into Android `BuildConfig`. The resulting APK calls Gemini directly and needs no desktop service. The key is extractable from the APK, so this build must not be distributed publicly. A production release should return to ephemeral tokens or a secured backend.

## Next production increments

1. Persist learner/profile/progress/reviews in Room and preferences in DataStore.
2. Convert curriculum to versioned JSON/YAML with validation and an authoring pipeline.
3. Add account-optional offline mode, then sync with a backend.
4. Expand the secure Gemini gateway with authentication, conversation scenarios, mistake memory, and audio-based structured feedback.
5. Integrate downloadable sherpa-onnx voice packs for offline Bengali/English recognition; retain system speech as fallback.
6. Complete qualified-teacher review and learner pilots for the 72 lessons, then expand the placement/review foundation with achievements and editable daily goals.
7. Validate Bengali pedagogy and every lesson with qualified teachers before release.

See [TESTING_GUIDE.md](TESTING_GUIDE.md), [UX_AUDIT.md](UX_AUDIT.md), [OPEN_SOURCE_EVALUATION.md](OPEN_SOURCE_EVALUATION.md), and [PRODUCT_PLAN.md](PRODUCT_PLAN.md).
