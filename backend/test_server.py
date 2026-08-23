import unittest

from server import format_tutor_reply, tutor_prompt


class TutorServerTest(unittest.TestCase):
    def test_tutor_prompt_accepts_bengali(self):
        system, prompt = tutor_prompt({"message": "আমি কীভাবে পরিচয় দেব?", "level": "A1"})
        self.assertIn("Bengali", system)
        self.assertIn("আমি কীভাবে পরিচয় দেব?", prompt)

    def test_tutor_prompt_rejects_empty_message(self):
        with self.assertRaises(ValueError):
            tutor_prompt({"message": "  "})

    def test_formats_structured_feedback(self):
        reply = format_tutor_reply(
            {
                "correctedEnglish": "I am going home.",
                "explanationBn": "going-এর আগে am দরকার।",
                "encouragementBn": "ভালো চেষ্টা!",
                "nextPromptEnglish": "Where do you live?",
            }
        )
        self.assertIn("Correct English: I am going home.", reply)
        self.assertIn("Your turn: Where do you live?", reply)


if __name__ == "__main__":
    unittest.main()
