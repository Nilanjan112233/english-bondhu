package com.englishbondhu.app.ai

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorReplyFormatterTest {
    @Test fun formatsAllStructuredFieldsInTeachingOrder() {
        val reply = TutorReplyFormatter.format(
            JSONObject()
                .put("correctedEnglish", "I went to the market yesterday.")
                .put("explanationBn", "অতীতের কাজের জন্য went ব্যবহার করুন।")
                .put("encouragementBn", "ভালো চেষ্টা!")
                .put("nextPromptEnglish", "What did you buy?"),
        )
        assertEquals(
            "Correct English: I went to the market yesterday.\n\n" +
                "অতীতের কাজের জন্য went ব্যবহার করুন।\n\n" +
                "ভালো চেষ্টা!\n\n" +
                "Your turn: What did you buy?",
            reply,
        )
    }

    @Test fun skipsBlankFieldsInsteadOfPrintingEmptyLabels() {
        val reply = TutorReplyFormatter.format(
            JSONObject().put("correctedEnglish", "  ").put("explanationBn", "ঠিক আছে।").put("nextPromptEnglish", ""),
        )
        assertEquals("ঠিক আছে।", reply)
        assertFalse(reply.contains("Correct English:"))
    }

    @Test fun joinsStructuredJsonSplitAcrossSeveralParts() {
        val body = """
            {"candidates":[{"content":{"parts":[
              {"text":"{\"correctedEnglish\":\"I am fine.\","},
              {"text":"\"explanationBn\":\"am লাগবে।\"}"}
            ]}}]}
        """.trimIndent()
        val text = TutorReplyFormatter.candidateText(body)
        assertEquals("I am fine.", JSONObject(text).getString("correctedEnglish"))
    }

    @Test fun ignoresThoughtParts() {
        val body = """
            {"candidates":[{"content":{"parts":[
              {"text":"thinking about grammar","thought":true},
              {"text":"{\"explanationBn\":\"ভালো।\"}"}
            ]}}]}
        """.trimIndent()
        assertEquals("{\"explanationBn\":\"ভালো।\"}", TutorReplyFormatter.candidateText(body))
    }

    @Test fun offlineTutorAnswersIntroductionRequests() = runBlocking {
        val reply = GuidedTutor().reply("How do I introduce myself?", "bn", "A1")
        assertTrue(reply.contains("Nice to meet you"))
    }
}
