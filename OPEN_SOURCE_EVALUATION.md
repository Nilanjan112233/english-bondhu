# Open-source foundation evaluation

Research date: 2026-07-19

## Decision

Build the Android app as a clean Jetpack Compose product, using official Android architecture as the reference. Reuse focused open-source engines and learning concepts behind interfaces. Do not fork an existing language-learning application as the product base.

No candidate found combines native Compose, Bengali-first instruction, full lessons/tests/speaking, extensible multilingual content, maintainability, and a permissive foundation license.

## Evaluated projects

| Project | Strength | Constraint | Decision |
|---|---|---|---|
| Android Now in Android | Production-quality Kotlin/Compose architecture; Apache-2.0 | News-domain reference, not language learning | Use architecture patterns, not its product code |
| LibreLingo | Open course format ideas, interactive exercises, progress, spaced repetition | Svelte web app; AGPL-3.0; audio/non-Latin work remains on its roadmap | Learn from content concepts; do not fork for native Android |
| AnkiDroid | Mature Android spaced repetition | Flashcard product; GPL family and large legacy/product surface | Implement/test a small scheduling domain; do not fork |
| sherpa-onnx | Apache-2.0, active, offline Android/Kotlin ASR/TTS/VAD; Bengali ASR model available | Native/model integration and model downloads add APK/storage complexity | Preferred production offline voice engine |
| whisper.cpp | MIT, Android example, multilingual transcription | Heavier general ASR; pronunciation scoring still requires product logic | Alternative/fallback experiment |
| Vosk API | Apache-2.0, mature lightweight offline Android ASR | Published language list historically did not include Bengali; newer Bengali model path is better served through sherpa-onnx | Not first choice |
| LanguageTool | Mature open-source English grammar/style checking | LGPL core and better suited to a self-hosted service than an APK | Optional backend grammar signal |
| llama.cpp | MIT and Android example for on-device LLM inference | Model size, RAM, latency, thermal load, and model-license variation | Later premium-device/offline experiment, not MVP default |

## Recommended stack

- App: Kotlin, Jetpack Compose, Material 3, single activity, UDF, ViewModels, coroutines/Flow.
- Offline data: Room for progress/review state; versioned content bundles for curriculum; DataStore for settings.
- Learning: exercise engine plus a tested SM-2-derived review scheduler. Scheduling parameters should be tuned from real learner outcomes.
- Speech MVP: Android `TextToSpeech` and `RecognizerIntent`, with clear capability/failure states.
- Offline speech production: downloadable `sherpa-onnx` Bengali and English packs, with model license and checksum recorded per release.
- Grammar: deterministic answer checking locally; optionally self-host LanguageTool for longer English writing.
- AI: server-side, provider-neutral tutor gateway. Structured responses should separate correction, Bengali explanation, example, and next prompt.

## Licensing rules before shipping

Every imported code component, model, voice, dataset, and curriculum asset needs its own inventory. Code license does not cover model weights or training data. Preserve notices, publish required changes where applicable, and do not assume a GitHub repository is reusable when it has no explicit license.

## Primary sources

- Now in Android: https://github.com/android/nowinandroid
- Android architecture recommendations: https://developer.android.com/topic/architecture/recommendations
- LibreLingo: https://github.com/kantord/LibreLingo
- AnkiDroid: https://github.com/ankidroid/Anki-Android
- sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx
- sherpa-onnx model index: https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html
- whisper.cpp Android example: https://github.com/ggml-org/whisper.cpp/tree/master/examples/whisper.android
- Vosk API: https://github.com/alphacep/vosk-api
- LanguageTool: https://github.com/languagetool-org/languagetool
- llama.cpp Android documentation: https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md
- Mozilla Common Voice datasets: https://commonvoice.mozilla.org/en/datasets
