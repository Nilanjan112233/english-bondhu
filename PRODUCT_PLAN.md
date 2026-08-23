# Product plan

## Product promise

Help a Bengali speaker use practical English in daily life through short guided lessons, active recall, realistic speaking, and a tutor that explains mistakes in simple Bengali. Later source languages use the same engine and independently reviewed curricula.

## Core learner loop

1. Set a goal and complete a short placement check.
2. Follow the next recommended lesson.
3. Learn meaning and grammar in the source language.
4. Listen, speak, read, and write—not only select answers.
5. Get immediate, specific correction.
6. Revisit weak items through spaced practice.
7. Use the skill in an AI role-play and periodic test.

## Curriculum shape

- CEFR-aligned paths: Pre-A1, A1, A2, B1 initially.
- Units: everyday needs, introductions, family, travel, work, health, shopping, phone calls, and emergencies.
- Lesson types: vocabulary, grammar in context, pronunciation, listening, reading, writing, dialogue, and review.
- Assessments: placement, unit checkpoint, mastery review, and four-skill level tests.
- Bengali support: Bengali explanations, contrastive grammar notes, common Bengali-speaker errors, optional transliteration only where pedagogically useful.

## AI responsibilities

- Explain a correction in the learner's source language.
- Run constrained role-play at the learner's level.
- Generate extra practice only from approved lesson objectives.
- Remember recurring mistake categories, not unnecessary conversation content.
- Produce structured feedback that the app can render and measure.

AI must not be the curriculum source of truth, independently award mastery, or silently replace reviewed lesson content. Learners need a visible fallback when the service is offline or a response cannot be trusted.

## Delivery phases

### Phase 1 — usable offline alpha

- Complete app navigation and design system *(prototype complete; accessibility polish remains)*
- Persist progress, placement, streak, mistakes, and review queue *(SharedPreferences prototype complete; Room migration remains)*
- 72 Bengali-to-English Pre-A1/A1 lessons implemented; qualified-teacher sign-off and learner piloting pending
- All four exercise modes plus system speech
- Placement mini-test and unit checkpoint *(placement prototype complete; teacher calibration remains)*
- Accessibility, Bengali font/device testing, and instrumentation tests

### Phase 2 — connected private beta

- Authentication and cross-device sync
- Secure AI tutor gateway and scenario conversations
- Structured AI corrections and cost/safety controls
- Downloadable offline speech models
- 100+ A1/A2 lessons and content authoring/validation tools
- Product analytics with explicit consent and deletion controls

### Phase 3 — launch and language expansion

- Full A1 and strong A2 Bengali curricula
- Teacher/admin content review workflow
- Subscription/entitlement system if required
- Hindi as the second source language to prove the abstraction
- Play Store quality, privacy, abuse, load, and recovery validation

## Definition of publishable

A build compiling successfully is not enough. Release requires curriculum review, automated unit/UI tests, real-device voice checks across low/mid/high hardware, offline/recovery behavior, backend security review, privacy disclosures, model/data license inventory, accessibility checks, and beta learner evidence that feedback is understandable and correct.

## Current improvement checkpoint

The app now records every guided lesson/practice answer, schedules it with SM-2, prioritizes due and recently incorrect items in Smart Review, and shows accuracy and weak-item counts. Placement recommends an entry point without controlling access: all 72 lessons remain open, skipped lessons are never marked complete, and the next recommendation follows the learner's last chosen lesson. Guided shopping, doctor, interview, and phone-call dialogues provide deterministic practice alongside the open-ended Gemini tutor.

The next content milestone is qualified-teacher review of the implemented 72 lessons, richer reading and pronunciation tasks, and empirical calibration of placement questions. The next engineering milestone is Room/DataStore migration plus UI/device tests and real-device microphone validation.
