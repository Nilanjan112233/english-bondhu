# Curriculum review register

## Current status

The application contains 72 runnable Bengali-to-English lessons: 12 units with 6 lessons each and 4 exercise modes per lesson, for 288 curriculum exercises. The progression is informed by CEFR Pre-A1/A1 communicative goals and uses Bengali explanations and contrastive notes.

Current code status: `INTERNAL_REVIEWED`.

This means the content has been structurally validated and internally checked for progression, English form, Bengali meaning, answer availability, and exercise completeness. It does **not** mean a qualified human teacher has signed it off. A lesson may be changed to `TEACHER_REVIEWED` only after the review record below is completed by a named reviewer.

## Unit inventory

| Unit | Theme | Lessons | Main learner outcome |
|---|---|---:|---|
| 1 | Sounds and first words | 6 | Recognise letters, spell a name, follow basic instructions |
| 2 | Meet people | 6 | Greet people and exchange personal information |
| 3 | Numbers, dates and time | 6 | Handle numbers, dates, clock time and schedules |
| 4 | Family and home | 6 | Describe family, possessions and a home |
| 5 | Daily life | 6 | Describe routines, negatives and frequency |
| 6 | Ask and connect | 6 | Ask questions, clarify and sustain a short exchange |
| 7 | Food and shopping | 6 | Order food, discuss amounts, sizes and prices |
| 8 | Places and travel | 6 | Ask/give directions and handle simple travel tasks |
| 9 | Health and safety | 6 | Describe symptoms, understand advice and request help |
| 10 | Work and services | 6 | Discuss work, ability, calls, forms and appointments |
| 11 | Past events and plans | 6 | Discuss simple past events and near-future plans |
| 12 | A1 integration | 6 | Read/write short messages and complete practical tasks |

## Required teacher checks

For each lesson, the reviewer should verify:

1. The stated can-do goal is appropriate for the assigned Pre-A1/A1 level.
2. English answers are grammatical, natural, and appropriate for South Asian adult learners.
3. Bengali prompts and explanations are accurate, clear, and not overly literal.
4. Distractors are unambiguously wrong and test the intended point.
5. Listening and speaking sentences are short enough for the level.
6. Gender, religion, occupation, family, disability, and regional examples are respectful and inclusive.
7. Health and emergency language is safe, practical, and does not imply medical diagnosis.
8. The placement sample is representative; score thresholds require learner data before high-stakes use.

## Sign-off record

| Field | Value |
|---|---|
| Reviewer name | Pending |
| Qualifications/experience | Pending |
| Bengali variety reviewed | Pending |
| Review date | Pending |
| Units approved | Pending |
| Corrections requested | Pending |
| Learner pilot sample | Pending |
| Final decision | Pending |

After sign-off, update only the approved lessons to `ContentReviewStatus.TEACHER_REVIEWED`, record the reviewer and date in this file, rerun all curriculum tests, and retain the reviewed content revision for auditability.

## Alignment references

- [Council of Europe CEFR descriptors](https://www.coe.int/en/web/common-european-framework-reference-languages/cefr-descriptors), including Pre-A1/A1 can-do descriptors.
- [Council of Europe Reference Level Descriptions](https://www.coe.int/en/web/common-european-framework-reference-languages/reference-level-descriptions), including the English Core Inventory route for language-specific grammar and vocabulary decisions.

CEFR is a descriptive framework, not an automatic certification. The application must not display the Council of Europe logo or claim Council of Europe approval.
