# English Bondhu user-experience audit

## Scope and evidence

This audit follows the app as a first-time Bengali-speaking learner: first launch, choosing a starting point, browsing 72 lessons, completing each exercise type, recovering from mistakes, reviewing weak material, using AI chat, and starting a Gemini Live conversation.

Evidence used:

- Direct review of the current Compose state and event flows.
- Curriculum/domain unit tests and Android compilation/lint.
- APK metadata and standalone credential-injection verification.
- Physical-device E2E on a Xiaomi 2201117SI running Android 13 at 1080×2400: clean install, first launch, free middle-course entry, all four exercise modes, saved progress, weak-item review, text Gemini, Gemini Live, restart, light/dark theme, 130% font, landscape, and offline recovery.
- Device screenshots and UI hierarchies are retained under `e2e-artifacts/`.

## Findings and v0.4 changes

| Journey | User problem found | Improvement |
|---|---|---|
| First launch | Home appeared without explaining placement, free lesson choice, or AI internet needs | Added a one-time Bengali welcome screen with three clear starting choices |
| Home | Fixed “good morning” text was wrong later in the day; “today's goal” sounded mandatory | Added time-aware greeting and changed the card to an optional recommendation |
| Find a lesson | Twelve collapsed units and 72 lessons were slow to scan | Added Bengali/English/topic search and an incomplete-only filter |
| Listening | The English transcript was visible before answering, defeating listening comprehension | Concealed the transcript until checking; retained automatic playback and replay |
| Written answers | Contractions and tiny spelling slips were marked wrong | Normalised common contractions and allowed tightly bounded English edit distance without accepting missing words |
| Wrong answer | Learner could only continue after seeing the correction | Added explicit retry and continue choices |
| Leave lesson | × could silently discard an unfinished session | Added confirmation for toolbar and Android Back exits |
| Finish lesson | The app returned immediately without a meaningful result moment | Added score, percentage, feedback, and an explicit save/return action |
| Smart Review | Weak-item counts were abstract | Added a short visible list of current weak prompts |
| AI chat | Keyboard Send did nothing, starter intent was unclear, and new replies could appear off-screen | Added working IME Send, quick starter prompts, and automatic scroll |
| Microphone | Granting permission required tapping the microphone again | Recognition now launches immediately after permission is granted |
| Live conversation | Long transcript did not follow the latest turn; audio transfer was not clearly disclosed | Added transcript auto-scroll, clear-history control, and an audio-to-Gemini privacy note |
| Lesson pronunciation | Default Android TTS was too fast for a Bengali-speaking beginner | Set US English playback to 72% speed with natural pitch and labeled replay as **ধীরে শুনুন** |
| Live connection | Gemini's setup response arrived as a binary WebSocket frame, leaving the app stuck on Connecting | Added binary-frame decoding, connection/setup progress, cancellation, and a 20-second actionable timeout |
| Live speaker echo | Gemini heard its own greeting through the phone speaker and entered a self-conversation loop | Added Android acoustic echo cancellation and discarded microphone upload while model audio is playing |
| Lesson feedback | A lesson-level grammar note could be unrelated to the choice or speaking sentence | Choice and speaking feedback now names the exact target sentence being practised |
| Build handoff | A failed Gradle task could be followed by stale APK/hash output | Build now invokes the Gradle wrapper directly and fails before printing any APK handoff |

## Remaining user risks

1. Bengali and English Android TTS/recognition quality still depends on services and voices installed on each phone.
2. Gemini Live passed speaker echo and microphone-lifecycle checks on this phone; Bluetooth routing, sustained barge-in, poor networks, and other manufacturers still need coverage.
3. Live learner transcripts are automatic and can misidentify scripts during rapid language switching even when Gemini understands the audio correctly.
4. Exact semantic alternatives beyond contractions/typos still need structured AI-assisted grading or authored accepted-answer lists.
5. The 72 lessons remain `INTERNAL_REVIEWED`; qualified Bengali/English teacher sign-off is pending.
6. SharedPreferences remains suitable for this personal prototype, but Room/DataStore is preferable before larger-scale use.

## Phone acceptance gate

The v0.4.3 checklist has been run on the attached Android phone and the discovered lesson-feedback, Live framing, timeout, and speaker-echo defects were corrected. Teacher review, wider device coverage, Bluetooth, and longer learner pilots remain release gates.
