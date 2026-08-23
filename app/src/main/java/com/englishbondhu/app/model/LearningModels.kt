package com.englishbondhu.app.model

enum class ExerciseType { CHOICE, LISTENING, SPEAKING, TRANSLATION }
enum class ContentReviewStatus { INTERNAL_REVIEWED, TEACHER_REVIEWED }

data class Exercise(
    val id: String,
    val type: ExerciseType,
    val instructionBn: String,
    val prompt: String,
    val answer: String,
    val choices: List<String> = emptyList(),
    val explanationBn: String,
)

data class Lesson(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val descriptionBn: String,
    val minutes: Int,
    val xp: Int,
    val exercises: List<Exercise>,
    val cefrLevel: String = "Pre-A1",
    val canDoBn: String = "",
    val reviewStatus: ContentReviewStatus = ContentReviewStatus.INTERNAL_REVIEWED,
)

data class LearningUnit(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val lessons: List<Lesson>,
)

data class Course(
    val id: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val level: String,
    val units: List<LearningUnit>,
)
