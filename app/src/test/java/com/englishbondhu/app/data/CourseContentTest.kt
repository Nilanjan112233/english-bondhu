package com.englishbondhu.app.data

import com.englishbondhu.app.model.ExerciseType
import com.englishbondhu.app.model.ContentReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseContentTest {
    private val course = SampleCourseRepository().activeCourse()
    private val lessons = course.units.flatMap { it.lessons }
    private val exercises = lessons.flatMap { it.exercises }

    @Test fun courseStartsFromZeroAndContainsSeventyTwoRunnableLessons() {
        assertEquals("letters-sounds", lessons.first().id)
        assertEquals(12, course.units.size)
        assertTrue(course.units.all { it.lessons.size == 6 })
        assertEquals(72, lessons.size)
        assertTrue(lessons.all { it.exercises.size == 4 })
    }

    @Test fun everyLessonPracticesAllFourExerciseModes() {
        lessons.forEach { lesson ->
            assertEquals(lesson.id, ExerciseType.entries.toSet(), lesson.exercises.map { it.type }.toSet())
        }
    }

    @Test fun contentIdentifiersAreUnique() {
        assertEquals(lessons.size, lessons.map { it.id }.toSet().size)
        assertEquals(exercises.size, exercises.map { it.id }.toSet().size)
        assertEquals(course.units.size, course.units.map { it.id }.toSet().size)
    }

    @Test fun placementAndDialoguesAreComplete() {
        val placement = PracticeContentRepository.placementTest(exercises)
        val dialogues = PracticeContentRepository.dialogueScenarios()
        assertEquals(12, placement.exercises.size)
        assertEquals(4, dialogues.size)
        assertTrue(dialogues.all { it.exercises.size == 3 })
        assertEquals(12, dialogues.flatMap { it.exercises }.map { it.id }.toSet().size)
    }

    @Test fun curriculumHasCanDoGoalsAndExplicitReviewStatus() {
        assertTrue(lessons.all { it.canDoBn.isNotBlank() })
        assertTrue(lessons.all { it.cefrLevel == "Pre-A1" || it.cefrLevel == "A1" })
        assertTrue(lessons.all { it.reviewStatus == ContentReviewStatus.INTERNAL_REVIEWED })
        assertTrue(exercises.all { it.prompt.isNotBlank() && it.answer.isNotBlank() && it.explanationBn.isNotBlank() })
    }

    @Test fun choiceAndListeningAnswersAreAlwaysAvailable() {
        exercises.filter { it.type == ExerciseType.CHOICE || it.type == ExerciseType.LISTENING }.forEach { exercise ->
            assertTrue(exercise.id, exercise.answer in exercise.choices)
            assertTrue(exercise.id, exercise.choices.size >= 3)
        }
    }

    @Test fun feedbackExplainsTheSentenceBeingPracticed() {
        exercises.filter { it.type == ExerciseType.CHOICE || it.type == ExerciseType.SPEAKING }
            .forEach { exercise ->
                assertTrue(exercise.id, exercise.explanationBn.contains(exercise.answer))
            }
    }
}
