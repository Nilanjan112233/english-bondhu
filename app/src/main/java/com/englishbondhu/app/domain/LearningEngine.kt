package com.englishbondhu.app.domain

import java.util.Locale
import kotlin.math.max

object AnswerChecker {
    private val contractions = mapOf(
        "i'm" to "i am", "you're" to "you are", "he's" to "he is", "she's" to "she is",
        "we're" to "we are", "they're" to "they are", "isn't" to "is not", "aren't" to "are not",
        "don't" to "do not", "doesn't" to "does not", "didn't" to "did not", "can't" to "cannot",
        "won't" to "will not", "i'll" to "i will", "i'd" to "i would", "it's" to "it is",
    )

    fun normalize(value: String): String {
        var normalized = value.lowercase(Locale.ROOT).replace('’', '\'')
        contractions.forEach { (short, expanded) ->
            normalized = normalized.replace(Regex("\\b${Regex.escape(short)}\\b"), expanded)
        }
        return normalized
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    }

    fun isCorrect(given: String, expected: String): Boolean {
        val actual = normalize(given)
        val target = normalize(expected)
        if (actual == target) return true
        if (!target.any { it in 'a'..'z' }) return false
        val actualWords = actual.split(' ')
        val targetWords = target.split(' ')
        if (actualWords.size != targetWords.size) return false
        val allowedDistance = if (target.length >= 24) 2 else 1
        return editDistance(actual, target) <= allowedDistance
    }

    private fun editDistance(first: String, second: String): Int {
        var previous = IntArray(second.length + 1) { it }
        first.forEachIndexed { firstIndex, firstChar ->
            val current = IntArray(second.length + 1)
            current[0] = firstIndex + 1
            second.forEachIndexed { secondIndex, secondChar ->
                current[secondIndex + 1] = minOf(
                    current[secondIndex] + 1,
                    previous[secondIndex + 1] + 1,
                    previous[secondIndex] + if (firstChar == secondChar) 0 else 1,
                )
            }
            previous = current
        }
        return previous.last()
    }
}

data class ReviewState(
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
)

data class PlacementResult(
    val correct: Int,
    val total: Int,
    val level: String,
    val levelBn: String,
    val recommendedLessonIndex: Int,
)

object PlacementEngine {
    fun evaluate(correct: Int, total: Int): PlacementResult {
        require(total > 0)
        require(correct in 0..total)
        val percentage = correct * 100 / total
        return when {
            percentage < 35 -> PlacementResult(correct, total, "Pre-A1 Starter", "একদম শুরু", 0)
            percentage < 60 -> PlacementResult(correct, total, "Pre-A1", "প্রাথমিক ভিত্তি", 12)
            percentage < 85 -> PlacementResult(correct, total, "A1 Beginner", "শুরুর ইংরেজি", 30)
            else -> PlacementResult(correct, total, "A1 Checkpoint", "A1 পুনরাবৃত্তি", 54)
        }
    }
}

data class LearningStats(
    val attempts: Int = 0,
    val correct: Int = 0,
    val dueReviews: Int = 0,
    val weakItems: Int = 0,
) {
    val accuracyPercent: Int get() = if (attempts == 0) 0 else correct * 100 / attempts
}

object SpacedRepetition {
    /** SM-2 scheduling. Quality is 0 (forgotten) through 5 (effortless). */
    fun review(previous: ReviewState, quality: Int): ReviewState {
        require(quality in 0..5)
        if (quality < 3) return previous.copy(repetitions = 0, intervalDays = 1)

        val repetitions = previous.repetitions + 1
        val interval = when (repetitions) {
            1 -> 1
            2 -> 6
            else -> max(1, (previous.intervalDays * previous.easeFactor).toInt())
        }
        val ease = max(
            1.3,
            previous.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)),
        )
        return ReviewState(repetitions, interval, ease)
    }
}
