package com.englishbondhu.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 3, 10)

    @Test fun firstEverLearningDayStartsAtOne() {
        assertEquals(1, StreakCalculator.afterLearning(null, 0, today))
    }

    @Test fun learningAgainTheSameDayKeepsTheStreak() {
        assertEquals(4, StreakCalculator.afterLearning(today, 4, today))
    }

    @Test fun learningOnConsecutiveDaysGrowsTheStreak() {
        assertEquals(5, StreakCalculator.afterLearning(today.minusDays(1), 4, today))
    }

    @Test fun missingADayRestartsTheStreak() {
        assertEquals(1, StreakCalculator.afterLearning(today.minusDays(2), 9, today))
    }

    @Test fun visibleStreakSurvivesUntilTheEndOfTheNextDay() {
        assertEquals(3, StreakCalculator.current(today, 3, today))
        assertEquals(3, StreakCalculator.current(today.minusDays(1), 3, today))
    }

    @Test fun visibleStreakDropsToZeroAfterAMissedDay() {
        assertEquals(0, StreakCalculator.current(today.minusDays(2), 7, today))
        assertEquals(0, StreakCalculator.current(null, 0, today))
    }
}
