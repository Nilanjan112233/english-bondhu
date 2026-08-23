package com.englishbondhu.app.data

import com.englishbondhu.app.model.Exercise
import com.englishbondhu.app.model.ExerciseType
import com.englishbondhu.app.model.Lesson

object PracticeContentRepository {
    fun placementTest(allExercises: List<Exercise>): Lesson {
        val selectedIds = listOf(
            "letters-sounds-choice", "pronouns-be-choice", "numbers-age-choice",
            "family-things-choice", "present-simple-choice", "questions-choice",
            "shopping-choice", "travel-directions-choice", "symptoms-choice",
            "abilities-can-choice", "past-checkpoint-choice", "a1-final-checkpoint-choice",
        )
        val byId = allExercises.associateBy { it.id }
        return Lesson(
            id = "placement-test",
            titleEn = "Placement test",
            titleBn = "আপনার স্তর যাচাই",
            descriptionBn = "১২টি প্রশ্নে কোথা থেকে শুরু করবেন তা জানুন",
            minutes = 10,
            xp = 0,
            exercises = selectedIds.mapNotNull(byId::get),
        )
    }

    fun dialogueScenarios(): List<Lesson> = listOf(
        scenario(
            "dialogue-shop", "Shopping conversation", "দোকানে কথোপকথন", "দাম জিজ্ঞাসা থেকে কেনা পর্যন্ত",
            line("ds1", "Shopkeeper: Good morning. How can I help you?", "বলুন: আমি একটি নীল শার্ট খুঁজছি।", "I am looking for a blue shirt.", "looking for মানে খুঁজছি।"),
            line("ds2", "Shopkeeper: Here is one. It is 800 taka.", "বলুন: একটু কম দাম হবে?", "Can you lower the price a little?", "lower the price মানে দাম কমানো।"),
            line("ds3", "Shopkeeper: I can make it 750 taka.", "বলুন: ঠিক আছে, আমি এটি নেব।", "Okay, I will take it.", "কিছু কেনার সিদ্ধান্তে I will take it বলা যায়।"),
        ),
        scenario(
            "dialogue-doctor", "At the doctor", "ডাক্তারের কাছে", "অসুস্থতা সহজ English-এ বলা",
            line("dd1", "Doctor: What is the problem?", "বলুন: আমার মাথা ব্যথা করছে।", "I have a headache.", "have a headache মানে মাথা ব্যথা থাকা।"),
            line("dd2", "Doctor: How long have you felt this way?", "বলুন: দুই দিন ধরে।", "For two days.", "সময়ের দৈর্ঘ্যের আগে for ব্যবহার করুন।"),
            line("dd3", "Doctor: Please take this medicine after food.", "বলুন: দিনে কতবার?", "How many times a day?", "ওষুধের frequency জানতে এই প্রশ্নটি কাজে লাগে।"),
        ),
        scenario(
            "dialogue-interview", "Simple interview", "সহজ ইন্টারভিউ", "নিজের পরিচয় এবং কাজের অভিজ্ঞতা",
            line("di1", "Interviewer: Please tell me about yourself.", "বলুন: আমার নাম রাহুল এবং আমি কলকাতায় থাকি।", "My name is Rahul and I live in Kolkata.", "নিজের নাম ও শহর এক বাক্যে বলা যায়।"),
            line("di2", "Interviewer: What are your strengths?", "বলুন: আমি পরিশ্রমী এবং সময়নিষ্ঠ।", "I am hardworking and punctual.", "strength বলতে নিজের ভালো গুণ বলুন।"),
            line("di3", "Interviewer: Why do you want this job?", "বলুন: আমি শিখতে এবং উন্নতি করতে চাই।", "I want to learn and grow.", "ছোট ও ইতিবাচক উত্তর দিন।"),
        ),
        scenario(
            "dialogue-phone", "Phone call", "ফোনে কথা বলা", "পরিচয়, কাউকে চাওয়া এবং বার্তা রাখা",
            line("dp1", "Caller: Hello, this is Mina speaking.", "বলুন: আমি কি মিস্টার দাসের সঙ্গে কথা বলতে পারি?", "May I speak to Mr Das?", "May I... একটি ভদ্র অনুরোধ।"),
            line("dp2", "Receptionist: He is not available now.", "বলুন: আমি কি একটি বার্তা রাখতে পারি?", "Can I leave a message?", "leave a message মানে বার্তা রেখে যাওয়া।"),
            line("dp3", "Receptionist: Certainly. What should I tell him?", "বলুন: দয়া করে আমাকে ফোন করতে বলবেন।", "Please ask him to call me.", "ask him to... দিয়ে অনুরোধ পৌঁছে দেওয়া যায়।"),
        ),
    )

    private fun scenario(id: String, en: String, bn: String, description: String, vararg exercises: Exercise) =
        Lesson(id, en, bn, description, 6, 15, exercises.toList())

    private fun line(id: String, context: String, instruction: String, answer: String, note: String) = Exercise(
        id = id,
        type = ExerciseType.TRANSLATION,
        instructionBn = instruction,
        prompt = context,
        answer = answer,
        explanationBn = note,
    )
}
