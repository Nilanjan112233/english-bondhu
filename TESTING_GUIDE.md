# Testing and APK installation guide

## 1. Test on a Windows desktop

This is an Android application, so it does not open directly as a normal Windows `.exe`. Test it on the desktop with the Android Emulator:

1. Install the latest stable Android Studio from https://developer.android.com/studio.
2. Open this project folder in Android Studio.
3. Let Gradle sync finish.
4. Open **Tools → Device Manager**.
5. Select **Create virtual device**, choose a Pixel phone, and install an Android 15 or Android 16 system image.
6. Start the virtual phone.
7. Select the `app` run configuration and press **Run ▶**.

The supplied standalone APK calls Gemini directly; no local gateway is needed. To rebuild that configuration:

```powershell
.\build-standalone-apk.ps1
```

Install the resulting APK in the running emulator by dragging it onto the emulator window, or run `install-apk.ps1`.

The local backend commands below remain available only as optional development diagnostics; they are not needed by the standalone app.

On this development machine, NovelForge already holds an encrypted Gemini credential. It can be reused in memory without copying it into this repository:

```powershell
py -3.11 backend\start_from_novelforge.py --novelforge-backend <path-to-NovelForge-backend>
```

Without NovelForge, start the same gateway with your own key (prompted, hidden input):

```powershell
.\start-ai-backend.ps1
```

This helper is development-only and depends on the local NovelForge backend, MongoDB, its encryption configuration, and Python 3.11 environment. A real deployment must give English Bondhu its own restricted credential and secret store.

Check the server independently:

```powershell
Invoke-RestMethod http://127.0.0.1:8787/health
```

## 2. Build an APK

Run:

```powershell
.\build-apk.ps1
```

The output is:

```text
app\build\outputs\apk\debug\app-debug.apk
```

For the personal standalone build that calls Gemini directly and does not need a desktop server:

```powershell
.\build-standalone-apk.ps1
```

This injects NovelForge's decrypted Gemini key into Android `BuildConfig` only during compilation. The key is not written to a source file, but it is embedded in—and extractable from—the resulting APK. Do not distribute this build publicly.

The verified handoff copy is named:

```text
app\build\outputs\apk\debug\EnglishBondhu-standalone-debug.apk
```

This is a debug APK for testing. A Play Store release needs a private signing key, release shrinking, HTTPS backend, privacy disclosures, and release validation.

## 3. Send the APK to a phone

You can copy `app-debug.apk` using USB, Google Drive, email, Telegram, or another file-transfer service. On the phone:

1. Open the downloaded APK.
2. If Android blocks it, allow **Install unknown apps** for the app that opened the file.
3. Confirm installation.
4. After installation, turn that permission off again if desired.

Do not publish or broadly share a debug APK. It is signed with a development key and permits cleartext traffic for local testing.

## 4. Install over USB

1. On the phone, enable **Developer options → USB debugging**.
2. Connect the phone and accept its authorization prompt.
3. Run:

```powershell
.\install-apk.ps1
```

## 5. Use Gemini from a physical phone

For the standalone personal APK, install it and connect the phone to the internet. The computer can be turned off; no shared Wi-Fi, LAN address, firewall rule, or hosted desktop process is required.

The older local-backend development configuration can still be built with a LAN address when diagnosing the gateway, but it is not the recommended personal test path.

<!-- Optional local-backend diagnostics:

```powershell
Get-NetIPAddress -AddressFamily IPv4 |
  Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } |
  Select-Object InterfaceAlias,IPAddress
```

Assuming the address is `192.168.1.50`:

```powershell
.\start-ai-backend.ps1 -BindAddress 0.0.0.0
.\build-apk.ps1 -AiBaseUrl http://192.168.1.50:8787
```

Then transfer/install the newly built APK. Windows Firewall may ask permission for Python; allow access only on trusted private networks. The LAN IP can change, so rebuild if it does.
-->

## 6. What voice correction currently means

- The learner can speak English through the microphone.
- Android converts speech into text.
- Gemini corrects grammar, vocabulary, tense, and natural wording and explains the correction in Bengali.
- Lesson prompts and AI English excerpts can be read aloud using Android text-to-speech; beginner playback uses a slower 72% rate and natural pitch.
- The Live tab streams the original microphone audio directly to Gemini and plays Gemini's native spoken response, allowing conversational pronunciation feedback when the audio is clear.
- Bengali typed questions are understood by Gemini.

The standard lesson and text-tutor microphone buttons use Android speech-to-text, so those two paths can correct the transcript but cannot reliably grade pronunciation, stress, rhythm, or accent. Use the **Live** tab for audio-aware conversation. Automated compilation and API handshake checks do not replace a real-device listening test; validate microphone capture, speaker output, echo, and Bengali/English turn-taking on the target phone.

## 7. Test the new adaptive-learning features

1. On Home, open **আপনার স্তর জানুন**, answer all 12 questions, and confirm that a level and score appear on Home.
2. Open Learn and confirm all 12 unit headers are visible and every one of the 72 lessons can be opened. No lesson should display a lock.
3. Complete placement and confirm its recommended unit opens initially with a ★ recommendation. Confirm other units remain available and skipped lessons are not marked complete.
4. Deliberately answer one lesson question incorrectly. Open Practice and confirm the weak-item count increases.
5. Open **আজকের smart review**. Recently incorrect items should be prioritized; correct the item and confirm the weak count drops.
6. Complete a lesson in the middle of the course and confirm Home recommends the following lesson instead of forcing lesson 1.
7. Complete more answers and confirm Home updates total attempts and accuracy after the session.
8. In Practice, run each scenario under **বাস্তব কথোপকথন**: shopping, doctor, interview, and phone call.

## 8. First-time-user and usability checks

For a clean first-launch test, uninstall the previous app before installing the new APK. This clears saved progress.

1. Confirm the welcome screen offers three understandable choices: placement, start from the beginning, or browse all lessons.
2. Choose Browse, search for `shopping`, and confirm only matching content opens. Turn on **শুধু অসমাপ্ত** and confirm completed lessons disappear.
3. Start a listening question. The spoken English must play, but its written transcript must remain hidden until the answer is checked. Confirm the replay button works.
4. Enter `I'm fine` where `I am fine` is expected and test one small English typo; both should be accepted. Missing or changed grammar words must remain incorrect.
5. Give a wrong answer and confirm both **আবার চেষ্টা** and **এগিয়ে যান** are available.
6. Press × or Android Back after beginning a lesson. Confirm the app warns before discarding the unfinished session.
7. Finish a session and confirm a score/summary screen appears before returning Home.
8. In AI Bondhu, use each quick starter, submit using the keyboard Send action, and confirm the conversation scrolls to the newest reply.
9. Deny microphone permission once, confirm the explanation is understandable, then grant it and confirm speech recognition starts without requiring an extra tap.
10. In Live, confirm the privacy note states that audio is sent to Gemini and that the transcript follows the latest turn.

The v0.4.3 pass was exercised on a Xiaomi 2201117SI running Android 13. The full device evidence is summarized in `UX_AUDIT.md` and captured in a local `e2e-artifacts/` folder (not committed, because it holds personal-device captures); repeat the checklist on other phone brands and Android versions before public release.
