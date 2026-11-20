package com.englishbondhu.app.data

import android.content.Context
import androidx.core.content.edit
import com.englishbondhu.app.domain.LearningStats
import com.englishbondhu.app.domain.PlacementEngine
import com.englishbondhu.app.domain.PlacementResult
import com.englishbondhu.app.domain.ReviewState
import com.englishbondhu.app.domain.SpacedRepetition
import com.englishbondhu.app.domain.StreakCalculator
import java.time.LocalDate

class ProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)

    fun completedLessonIds(): Set<String> =
        preferences.getStringSet("completed_lessons", emptySet())?.toSet().orEmpty()

    fun markLessonCompleted(lessonId: String) {
        val updated = completedLessonIds() + lessonId
        touchLearningDay()
        preferences.edit {
            putStringSet("completed_lessons", updated)
            putString("last_completed_lesson", lessonId)
        }
    }

    fun recordExerciseAttempt(exerciseId: String, answer: String, correct: Boolean) {
        val old = reviewState(exerciseId)
        val next = SpacedRepetition.review(old, if (correct) 5 else 2)
        val ids = reviewIds() + exerciseId
        val weakIds = if (correct) weakExerciseIds() - exerciseId else weakExerciseIds() + exerciseId
        val prefix = "review_$exerciseId"
        preferences.edit {
            putStringSet("review_ids", ids)
            putStringSet("weak_exercise_ids", weakIds)
            putInt("${prefix}_repetitions", next.repetitions)
            putInt("${prefix}_interval", next.intervalDays)
            putString("${prefix}_ease", next.easeFactor.toString())
            putLong("${prefix}_due", LocalDate.now().plusDays(next.intervalDays.toLong()).toEpochDay())
            putString("${prefix}_answer", answer.take(160))
            putInt("attempts", preferences.getInt("attempts", 0) + 1)
            putInt("correct_attempts", preferences.getInt("correct_attempts", 0) + if (correct) 1 else 0)
        }
        touchLearningDay()
    }

    fun weakExerciseIds(): Set<String> =
        preferences.getStringSet("weak_exercise_ids", emptySet())?.toSet().orEmpty()

    fun dueExerciseIds(today: LocalDate = LocalDate.now()): Set<String> = reviewIds().filterTo(mutableSetOf()) { id ->
        preferences.getLong("review_${id}_due", Long.MAX_VALUE) <= today.toEpochDay()
    }

    fun learningStats(): LearningStats = LearningStats(
        attempts = preferences.getInt("attempts", 0),
        correct = preferences.getInt("correct_attempts", 0),
        dueReviews = dueExerciseIds().size,
        weakItems = weakExerciseIds().size,
    )

    fun savePlacement(correct: Int, total: Int) {
        val result = PlacementEngine.evaluate(correct, total)
        preferences.edit {
            putInt("placement_correct", correct)
            putInt("placement_total", total)
            putString("placement_level", result.level)
            putString("placement_level_bn", result.levelBn)
            putInt("placement_lesson_index", result.recommendedLessonIndex)
        }
    }

    fun placementResult(): PlacementResult? {
        val total = preferences.getInt("placement_total", 0)
        if (total <= 0) return null
        return PlacementEngine.evaluate(preferences.getInt("placement_correct", 0).coerceIn(0, total), total)
    }

    private fun reviewIds(): Set<String> =
        preferences.getStringSet("review_ids", emptySet())?.toSet().orEmpty()

    private fun reviewState(exerciseId: String): ReviewState {
        val prefix = "review_$exerciseId"
        return ReviewState(
            repetitions = preferences.getInt("${prefix}_repetitions", 0),
            intervalDays = preferences.getInt("${prefix}_interval", 0),
            easeFactor = preferences.getString("${prefix}_ease", "2.5")?.toDoubleOrNull() ?: 2.5,
        )
    }

    private fun lastLearningDay(): LocalDate? =
        preferences.getString("last_learning_day", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun touchLearningDay() {
        val today = LocalDate.now()
        val streak = StreakCalculator.afterLearning(lastLearningDay(), preferences.getInt("streak", 0), today)
        preferences.edit {
            putString("last_learning_day", today.toString())
            putInt("streak", streak)
        }
    }

    /** The visible streak resets to 0 once a full day has been missed. */
    fun streak(today: LocalDate = LocalDate.now()): Int =
        StreakCalculator.current(lastLearningDay(), preferences.getInt("streak", 0), today)

    fun lastCompletedLessonId(): String? = preferences.getString("last_completed_lesson", null)

    fun hasSeenWelcome(): Boolean = preferences.getBoolean("has_seen_welcome", false)

    fun markWelcomeSeen() = preferences.edit { putBoolean("has_seen_welcome", true) }
}
