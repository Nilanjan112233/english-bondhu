package com.englishbondhu.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningEngineTest {
    @Test fun answerCheckerIgnoresCaseWhitespaceAndPunctuation() {
        assertTrue(AnswerChecker.isCorrect("  I AM fine! ", "I am fine."))
        assertFalse(AnswerChecker.isCorrect("I have fine", "I am fine."))
    }

    @Test fun answerCheckerAcceptsContractionsAndSmallTypos() {
        assertTrue(AnswerChecker.isCorrect("I'm fine", "I am fine."))
        assertTrue(AnswerChecker.isCorrect("I stud English every day", "I study English every day."))
        assertFalse(AnswerChecker.isCorrect("I study English tomorrow", "I study English every day."))
        assertFalse(AnswerChecker.isCorrect("I am student", "I am a student."))
    }

    @Test fun failedReviewReturnsToOneDay() {
        val result = SpacedRepetition.review(ReviewState(4, 30, 2.5), 2)
        assertEquals(0, result.repetitions)
        assertEquals(1, result.intervalDays)
    }

    @Test fun successfulReviewsGrowIntervals() {
        val first = SpacedRepetition.review(ReviewState(), 5)
        val second = SpacedRepetition.review(first, 5)
        val third = SpacedRepetition.review(second, 5)
        assertEquals(1, first.intervalDays)
        assertEquals(6, second.intervalDays)
        assertTrue(third.intervalDays > second.intervalDays)
    }

    @Test fun placementMapsScoresToUsefulStartingPoints() {
        assertEquals(0, PlacementEngine.evaluate(2, 12).recommendedLessonIndex)
        assertEquals(12, PlacementEngine.evaluate(5, 12).recommendedLessonIndex)
        assertEquals(30, PlacementEngine.evaluate(8, 12).recommendedLessonIndex)
        assertEquals(54, PlacementEngine.evaluate(11, 12).recommendedLessonIndex)
    }

    @Test fun learningStatsCalculatesAccuracySafely() {
        assertEquals(0, LearningStats().accuracyPercent)
        assertEquals(75, LearningStats(attempts = 4, correct = 3).accuracyPercent)
    }
}
