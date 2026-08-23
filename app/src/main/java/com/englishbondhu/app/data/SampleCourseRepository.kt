package com.englishbondhu.app.data

import com.englishbondhu.app.model.ContentReviewStatus
import com.englishbondhu.app.model.Course
import com.englishbondhu.app.model.Exercise
import com.englishbondhu.app.model.ExerciseType
import com.englishbondhu.app.model.LearningUnit
import com.englishbondhu.app.model.Lesson

interface CourseRepository { fun activeCourse(): Course }

private data class LessonSeed(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val canDoBn: String,
    val firstBn: String,
    val firstEn: String,
    val wrongOne: String,
    val wrongTwo: String,
    val secondBn: String,
    val secondEn: String,
    val noteBn: String,
    val level: String,
)

private data class UnitSeed(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val lessons: List<LessonSeed>,
)

class SampleCourseRepository : CourseRepository {
    override fun activeCourse() = Course(
        id = "bn-en-prea1-a1-v2",
        sourceLanguage = "বাংলা",
        targetLanguage = "English",
        level = "Pre-A1 → A1 • ৭২টি লেসন",
        units = curriculum.map { unit ->
            LearningUnit(unit.id, unit.titleEn, unit.titleBn, unit.lessons.map(::lesson))
        },
    )

    private fun lesson(seed: LessonSeed): Lesson {
        val choices = listOf(seed.wrongOne, seed.firstEn, seed.wrongTwo).distinct().shuffled()
        val listeningChoices = listOf(seed.secondBn, seed.firstBn, "উপরের কোনোটিই নয়").distinct().shuffled()
        return Lesson(
            id = seed.id,
            titleEn = seed.titleEn,
            titleBn = seed.titleBn,
            descriptionBn = seed.canDoBn,
            minutes = if (seed.level == "A1") 10 else 8,
            xp = if (seed.level == "A1") 30 else 20,
            exercises = listOf(
                Exercise(
                    "${seed.id}-choice", ExerciseType.CHOICE, "সঠিক English বাক্যটি বেছে নিন",
                    seed.firstBn, seed.firstEn, choices,
                    "সঠিক বাক্য: ${seed.firstEn} — বাংলা অর্থ: ${seed.firstBn}",
                ),
                Exercise("${seed.id}-translation", ExerciseType.TRANSLATION, "ইংরেজিতে লিখুন", seed.secondBn, seed.secondEn, explanationBn = seed.noteBn),
                Exercise("${seed.id}-listening", ExerciseType.LISTENING, "শুনুন এবং বাংলা অর্থ বেছে নিন", seed.secondEn, seed.secondBn, listeningChoices, seed.noteBn),
                Exercise(
                    "${seed.id}-speaking", ExerciseType.SPEAKING, "মাইক্রোফোনে বাক্যটি বলুন",
                    seed.firstEn, seed.firstEn,
                    explanationBn = "লক্ষ্য বাক্য: ${seed.firstEn} ধীরে ও পরিষ্কারভাবে বলুন।",
                ),
            ),
            cefrLevel = seed.level,
            canDoBn = seed.canDoBn,
            reviewStatus = ContentReviewStatus.INTERNAL_REVIEWED,
        )
    }

    private fun s(
        id: String, titleEn: String, titleBn: String, canDoBn: String,
        firstBn: String, firstEn: String, wrongOne: String, wrongTwo: String,
        secondBn: String, secondEn: String, noteBn: String, level: String = "Pre-A1",
    ) = LessonSeed(id, titleEn, titleBn, canDoBn, firstBn, firstEn, wrongOne, wrongTwo, secondBn, secondEn, noteBn, level)

    private val curriculum = listOf(
        UnitSeed("sounds-first-words", "Sounds and first words", "ধ্বনি ও প্রথম শব্দ", listOf(
            s("letters-sounds", "The English alphabet", "ইংরেজি বর্ণমালা", "A–Z চিনতে এবং নিজের নামের অক্ষর বলতে পারবেন।", "এটি A অক্ষর।", "This is the letter A.", "This are the letter A.", "These is letter A.", "আমার নাম রিনা।", "My name is Rina.", "letter-এর আগে the এবং নাম বলার জন্য My name is ব্যবহার করুন।"),
            s("vowel-sounds", "Five vowel letters", "পাঁচটি vowel", "A, E, I, O, U চিনতে এবং সহজ vowel শব্দ বলতে পারবেন।", "A দিয়ে apple শুরু হয়।", "A is for apple.", "A are for apple.", "A for is apple.", "E দিয়ে egg শুরু হয়।", "E is for egg.", "A, E, I, O, U হলো vowel letters।"),
            s("common-consonants", "Common consonant sounds", "পরিচিত consonant ধ্বনি", "B, C, D, M, S-এর পরিচিত শব্দ বলতে পারবেন।", "B দিয়ে book শুরু হয়।", "B is for book.", "B are a book.", "B for book is.", "M দিয়ে mother শুরু হয়।", "M is for mother.", "শব্দের প্রথম ধ্বনি শুনে letter মিলান।"),
            s("spell-your-name", "Spell your name", "নিজের নামের বানান", "নিজের নাম ধীরে ধীরে spell করতে পারবেন।", "আমার নামের বানান R-I-N-A।", "I spell my name R-I-N-A.", "I am spell R-I-N-A.", "My spell is name R-I-N-A.", "আপনার নামের বানান কী?", "How do you spell your name?", "spell করার সময় প্রতিটি letter আলাদা বলুন।"),
            s("classroom-words", "Classroom instructions", "ক্লাসের সহজ নির্দেশ", "listen, repeat, read, write-এর নির্দেশ বুঝতে পারবেন।", "অনুগ্রহ করে শুনুন।", "Please listen.", "Please listening.", "You please listens.", "বাক্যটি আবার বলুন।", "Please repeat the sentence.", "Please বাক্যকে ভদ্র করে; verb-এর মূল রূপ ব্যবহার করুন।"),
            s("sounds-checkpoint", "Sounds checkpoint", "ধ্বনি পুনরাবৃত্তি", "letter, spelling এবং classroom English একসঙ্গে ব্যবহার করতে পারবেন।", "দয়া করে আপনার নাম লিখুন।", "Please write your name.", "Please wrote your name.", "Write please is your name.", "আমি শব্দটি বুঝতে পারছি না।", "I do not understand the word.", "না বোঝালে I do not understand... বলা যায়।"),
        )),
        UnitSeed("meet-people", "Meet people", "পরিচয় ও শুভেচ্ছা", listOf(
            s("pronouns-be", "I, you, he and she", "I, you, he, she", "ব্যক্তি অনুযায়ী am, is, are ব্যবহার করতে পারবেন।", "আমি একজন ছাত্র।", "I am a student.", "I is a student.", "I are a student.", "সে আমার বন্ধু।", "She is my friend.", "I-এর সঙ্গে am; he/she-এর সঙ্গে is।"),
            s("greetings", "Greetings", "শুভেচ্ছা", "সময় অনুযায়ী hello, good morning এবং goodbye বলতে পারবেন।", "সুপ্রভাত।", "Good morning.", "Good night morning.", "Morning is good you.", "আবার দেখা হবে।", "See you again.", "Good morning অভিবাদন; Good night বিদায়ের সময় বলা হয়।"),
            s("introduce-yourself", "Introduce yourself", "নিজের পরিচয়", "নাম, দেশ এবং শহর বলে সংক্ষিপ্ত পরিচয় দিতে পারবেন।", "আমার নাম আরিফ।", "My name is Arif.", "My name are Arif.", "I name is Arif.", "আমি বাংলাদেশ থেকে এসেছি।", "I am from Bangladesh.", "I am from + place দিয়ে উৎস বা দেশ বলুন।"),
            s("ask-names", "Ask someone's name", "নাম জিজ্ঞাসা", "ভদ্রভাবে কারও নাম জিজ্ঞাসা ও উত্তর দিতে পারবেন।", "আপনার নাম কী?", "What is your name?", "What your name is?", "Who name are you?", "আমাকে মিতা বলে ডাকতে পারেন।", "You can call me Mita.", "What is your name? একটি পূর্ণ প্রশ্ন।"),
            s("feelings", "How are you?", "কেমন আছেন", "ভালো-মন্দ অনুভূতি সহজভাবে বলতে পারবেন।", "আমি ভালো আছি।", "I am fine.", "I fine am.", "I is fine.", "আজ আমি একটু ক্লান্ত।", "I am a little tired today.", "অনুভূতির adjective-এর আগে be verb বসে।"),
            s("personal-info-review", "Personal information", "ব্যক্তিগত তথ্য পুনরাবৃত্তি", "নাম, দেশ, ভাষা ও অনুভূতি নিয়ে ছোট কথোপকথন করতে পারবেন।", "আমি বাংলা বলি।", "I speak Bengali.", "I am speak Bengali.", "I speaks Bengali.", "আপনার সঙ্গে পরিচিত হয়ে ভালো লাগলো।", "Nice to meet you.", "ভাষার নামের আগে সাধারণত a/an লাগে না।"),
        )),
        UnitSeed("numbers-time", "Numbers, dates and time", "সংখ্যা, তারিখ ও সময়", listOf(
            s("numbers-one-twenty", "Numbers 1–20", "১ থেকে ২০", "১–২০ পর্যন্ত সংখ্যা বুঝতে ও বলতে পারবেন।", "আমার কাছে দশটি বই আছে।", "I have ten books.", "I has ten books.", "I have tenth book.", "ঘরে বারোজন মানুষ আছে।", "There are twelve people in the room.", "একটির বেশি হলে countable noun সাধারণত plural হয়।"),
            s("numbers-age", "Age and phone numbers", "বয়স ও ফোন নম্বর", "বয়স এবং ফোন নম্বর স্পষ্টভাবে বলতে পারবেন।", "আমার বয়স বিশ বছর।", "I am twenty years old.", "I have twenty years old.", "I is twenty old.", "আমার ফোন নম্বর এক দুই তিন।", "My phone number is one two three.", "ফোন নম্বরের digit আলাদা করে বলা যায়।"),
            s("days-week", "Days of the week", "সপ্তাহের দিন", "আজ, আগামীকাল এবং সপ্তাহের দিন বলতে পারবেন।", "আজ সোমবার।", "Today is Monday.", "Today Monday are.", "Today has Monday.", "আগামীকাল মঙ্গলবার।", "Tomorrow is Tuesday.", "দিনের নাম capital letter দিয়ে শুরু হয়।"),
            s("dates-months", "Dates and months", "তারিখ ও মাস", "সহজ তারিখ ও জন্মদিন বলতে পারবেন।", "আমার জন্মদিন মে মাসে।", "My birthday is in May.", "My birthday on May.", "My birthday are May.", "আজ মে মাসের পাঁচ তারিখ।", "Today is the fifth of May.", "মাসের আগে in; নির্দিষ্ট দিনের আগে on ব্যবহার হয়।"),
            s("clock-time", "Tell the time", "ঘড়ির সময়", "পূর্ণ ঘণ্টা, সাড়ে এবং মিনিটসহ সময় বলতে পারবেন।", "এখন সাতটা বাজে।", "It is seven o'clock.", "It are seven o'clock.", "This has seven clock.", "এখন সাড়ে সাতটা।", "It is half past seven.", "half past seven মানে 7:30।"),
            s("time-routine", "Schedules", "সময়সূচি", "কখন কোনো কাজ হয় তা জিজ্ঞাসা ও বলতে পারবেন।", "ক্লাসটি সকাল নয়টায় শুরু হয়।", "The class starts at nine in the morning.", "The class start on nine.", "Class is starting yesterday.", "দোকানটি কখন খোলে?", "When does the shop open?", "নির্দিষ্ট সময়ের আগে at ব্যবহার করুন।"),
        )),
        UnitSeed("family-home", "Family and home", "পরিবার ও বাড়ি", listOf(
            s("family-people", "Family members", "পরিবারের মানুষ", "পরিবারের সদস্যদের পরিচয় করাতে পারবেন।", "এটি আমার মা।", "This is my mother.", "This are my mother.", "These is mother mine.", "আমার একটি বড় বোন আছে।", "I have an older sister.", "একজনকে দেখাতে This is... ব্যবহার করুন।"),
            s("family-things", "Have and has", "have ও has", "কার কী আছে তা বলতে পারবেন।", "আমার একজন ভাই আছে।", "I have a brother.", "I has a brother.", "I am a brother.", "তার একটি নতুন ফোন আছে।", "She has a new phone.", "I/you/we/they-এর সঙ্গে have; he/she/it-এর সঙ্গে has।"),
            s("possessives", "My, your, his and her", "আমার, আপনার, তার", "my, your, his, her দিয়ে মালিকানা বোঝাতে পারবেন।", "এটি আমার ব্যাগ।", "This is my bag.", "This is me bag.", "This my is bag.", "ওটি তার ছাতা।", "That is her umbrella.", "possessive word noun-এর আগে বসে।"),
            s("this-that", "This, that, these, those", "কাছের ও দূরের জিনিস", "এক বা একাধিক কাছের/দূরের জিনিস দেখাতে পারবেন।", "এটি একটি চেয়ার।", "This is a chair.", "This are a chair.", "These is chair.", "ওগুলো আমার জুতা।", "Those are my shoes.", "this/that একবচন; these/those বহুবচন।"),
            s("there-is-are", "There is and there are", "কোথায় কী আছে", "ঘর বা জায়গায় কী আছে তা বলতে পারবেন।", "ঘরে একটি টেবিল আছে।", "There is a table in the room.", "There are a table in room.", "It has table the room.", "রান্নাঘরে দুটি জানালা আছে।", "There are two windows in the kitchen.", "একটির জন্য there is; একাধিকের জন্য there are।"),
            s("describe-home", "Describe your home", "নিজের বাড়ির বর্ণনা", "সহজ adjective ও place word দিয়ে বাড়ি বর্ণনা করতে পারবেন।", "আমার বাড়ি ছোট কিন্তু পরিষ্কার।", "My home is small but clean.", "My home are small clean.", "My home has small but.", "বাড়ির পাশে একটি দোকান আছে।", "There is a shop next to the house.", "দুটি বিপরীত ধারণা জুড়তে but ব্যবহার করুন।"),
        )),
        UnitSeed("daily-life", "Daily life", "প্রতিদিনের জীবন", listOf(
            s("daily-needs", "Needs and wants", "প্রয়োজন ও চাওয়া", "নিজের সহজ প্রয়োজন ও ইচ্ছা বলতে পারবেন।", "আমার পানি দরকার।", "I need water.", "I am need water.", "I needs water.", "আমি এক কাপ চা চাই।", "I want a cup of tea.", "need প্রয়োজন; want ইচ্ছা বোঝায়।"),
            s("present-simple", "Everyday actions", "প্রতিদিনের কাজ", "নিজের নিয়মিত কাজ present simple-এ বলতে পারবেন।", "আমি প্রতিদিন কাজ করি।", "I work every day.", "I am work every day.", "I worked every tomorrow.", "আমি সকালে ইংরেজি পড়ি।", "I study English in the morning.", "অভ্যাসের জন্য verb-এর base form ব্যবহার করুন।", "A1"),
            s("third-person", "He and she routines", "he/she-এর দৈনিক কাজ", "he/she-এর অভ্যাসে verb-এর s/es ব্যবহার করতে পারবেন।", "সে স্কুলে যায়।", "He goes to school.", "He go to school.", "He going to school.", "মিতা সকাল আটটায় কাজ শুরু করে।", "Mita starts work at eight.", "he/she/name-এর present verb-এ সাধারণত s/es যোগ হয়।", "A1"),
            s("present-negative", "Do not and does not", "বর্তমান কালের না-বাচক বাক্য", "যা করেন না তা বলতে পারবেন।", "আমি কফি খাই না।", "I do not drink coffee.", "I does not drink coffee.", "I not drinks coffee.", "সে রবিবার কাজ করে না।", "She does not work on Sunday.", "does not-এর পরে verb-এর base form থাকে।", "A1"),
            s("frequency", "Always, usually, sometimes", "কতবার কাজ করি", "frequency word দিয়ে অভ্যাসের হার বলতে পারবেন।", "আমি সাধারণত বাসে যাই।", "I usually go by bus.", "I go usually by bus am.", "I usual goes bus.", "আমি মাঝে মাঝে বাইরে খাই।", "I sometimes eat outside.", "frequency adverb সাধারণত main verb-এর আগে বসে।", "A1"),
            s("daily-sequence", "A day from morning to night", "সকাল থেকে রাত", "first, then, after that দিয়ে দৈনিক কাজ সাজাতে পারবেন।", "প্রথমে আমি নাশতা করি।", "First, I have breakfast.", "First, I am breakfast.", "I first has breakfast.", "তারপর আমি কাজে যাই।", "Then I go to work.", "ঘটনার ক্রমে first, then, after that ব্যবহার করুন।", "A1"),
        )),
        UnitSeed("ask-connect", "Ask and connect", "প্রশ্ন ও কথোপকথন", listOf(
            s("yes-no-questions", "Yes or no questions", "হ্যাঁ-না প্রশ্ন", "do/does দিয়ে সহজ প্রশ্ন করতে পারবেন।", "আপনি কি এখানে কাজ করেন?", "Do you work here?", "Does you work here?", "You work here do?", "সে কি ইংরেজি বলে?", "Does she speak English?", "does-এর পরে মূল verb-এ s থাকে না।", "A1"),
            s("questions", "What and who", "what ও who প্রশ্ন", "বস্তু, কাজ ও ব্যক্তি সম্পর্কে জানতে পারবেন।", "আপনি কী করেন?", "What do you do?", "What you do?", "What does you do?", "ওই মানুষটি কে?", "Who is that person?", "what জিনিস/তথ্য; who ব্যক্তি জানতে ব্যবহার হয়।", "A1"),
            s("where-questions", "Where questions", "where দিয়ে স্থান", "কেউ কোথায় থাকে বা কিছু কোথায় আছে জিজ্ঞাসা করতে পারবেন।", "আপনি কোথায় থাকেন?", "Where do you live?", "Where you live?", "Where does you live?", "নিকটতম ব্যাংক কোথায়?", "Where is the nearest bank?", "কাজের প্রশ্নে do; be verb-এর প্রশ্নে is/are সামনে আসে।", "A1"),
            s("when-why-how", "When, why and how", "কখন, কেন ও কীভাবে", "সময়, কারণ ও পদ্ধতি সম্পর্কে প্রশ্ন করতে পারবেন।", "আপনি কখন বাড়ি যান?", "When do you go home?", "When you go home?", "When does you goes home?", "আপনি কীভাবে কাজে যান?", "How do you go to work?", "when সময়; why কারণ; how পদ্ধতি জানতে ব্যবহার হয়।", "A1"),
            s("clarify-repeat", "Ask for clarification", "আবার বলতে অনুরোধ", "না বুঝলে পুনরাবৃত্তি বা ধীরে বলতে অনুরোধ করতে পারবেন।", "দয়া করে আবার বলবেন?", "Could you say that again, please?", "You could again say please?", "Did you saying it again?", "একটু ধীরে বলবেন?", "Could you speak more slowly?", "Could you... একটি ভদ্র অনুরোধ।", "A1"),
            s("short-conversation", "Keep a conversation going", "কথা এগিয়ে নেওয়া", "ছোট follow-up question দিয়ে কথোপকথন চালাতে পারবেন।", "সত্যি? তারপর কী হলো?", "Really? What happened next?", "Real? What next happening?", "Really what did next?", "আপনি এটা পছন্দ করেন কেন?", "Why do you like it?", "আগ্রহ দেখাতে Really? এবং follow-up question ব্যবহার করুন।", "A1"),
        )),
        UnitSeed("food-shopping", "Food and shopping", "খাবার ও কেনাকাটা", listOf(
            s("food-drink", "Food and drink", "খাবার ও পানীয়", "পরিচিত খাবার ও পানীয় চাইতে পারবেন।", "আমি ভাত ও সবজি খাই।", "I eat rice and vegetables.", "I eats rice and vegetable.", "I am eat rice.", "আমি একটু পানি চাই।", "I would like some water.", "ভদ্রভাবে চাইতে I would like... ব্যবহার করুন।", "A1"),
            s("countable-amounts", "One, two and many", "গণনাযোগ্য জিনিস", "one/two এবং singular/plural ঠিকভাবে ব্যবহার করতে পারবেন।", "আমার দুটি আপেল দরকার।", "I need two apples.", "I need two apple.", "I needs an apples.", "দয়া করে আমাকে একটি কমলা দিন।", "Please give me an orange.", "একটির জন্য a/an; একাধিকের জন্য plural noun।", "A1"),
            s("some-any", "Some and any", "some ও any", "খাবার আছে কি না জিজ্ঞাসা ও বলতে পারবেন।", "আমাদের কাছে কিছু দুধ আছে।", "We have some milk.", "We has any milk.", "We are some milk.", "আপনার কাছে কোনো ডিম আছে?", "Do you have any eggs?", "হ্যাঁ-বাচক বাক্যে some; প্রশ্ন/না-বাচকে সাধারণত any।", "A1"),
            s("shopping", "Ask the price", "দাম জিজ্ঞাসা", "দাম জানতে ও সিদ্ধান্ত জানাতে পারবেন।", "এটার দাম কত?", "How much is this?", "How many price is this?", "What this cost are?", "আমি এটি কিনতে চাই।", "I would like to buy this.", "দাম জানতে How much...? ব্যবহার করুন।", "A1"),
            s("sizes-quantities", "Sizes and quantities", "মাপ ও পরিমাণ", "size, colour ও quantity চেয়ে নিতে পারবেন।", "আপনার কাছে কি ছোট মাপ আছে?", "Do you have a smaller size?", "Are you have small size?", "Do you has a size smaller?", "আমার এক কিলো চাল দরকার।", "I need one kilo of rice.", "পরিমাণের পরে of + noun ব্যবহার হয়।", "A1"),
            s("restaurant", "At a restaurant", "রেস্তোরাঁয়", "খাবার order, বিল চাওয়া ও সমস্যা বলা পারবেন।", "আমি চিকেনটি নেব।", "I will have the chicken.", "I am have the chicken.", "I will has chicken.", "বিলটি দেবেন, দয়া করে।", "Could I have the bill, please?", "restaurant-এ order দিতে I will have... স্বাভাবিক।", "A1"),
        )),
        UnitSeed("places-travel", "Places and travel", "জায়গা ও যাতায়াত", listOf(
            s("town-places", "Places in town", "শহরের জায়গা", "bank, hospital, station-এর মতো জায়গা চিনতে পারবেন।", "পোস্ট অফিসটি ব্যাংকের পাশে।", "The post office is next to the bank.", "Post office next bank is.", "The post office are beside bank.", "হাসপাতালটি এখান থেকে দূরে।", "The hospital is far from here.", "next to পাশে; far from দূরে বোঝায়।", "A1"),
            s("place-prepositions", "In, on, under and between", "স্থানের preposition", "কোনো জিনিসের অবস্থান বলতে পারবেন।", "চাবিটি টেবিলের উপর।", "The key is on the table.", "The key are in table.", "Key is table on.", "ব্যাগটি দুটি চেয়ারের মাঝে।", "The bag is between the two chairs.", "between দুটি জিনিসের মাঝের অবস্থান বোঝায়।", "A1"),
            s("travel-directions", "Ask for directions", "পথ জিজ্ঞাসা", "ভদ্রভাবে পথ জিজ্ঞাসা করতে পারবেন।", "স্টেশন কোথায়?", "Where is the station?", "Where station are?", "What is station where?", "আমি কীভাবে বাসস্ট্যান্ডে যাব?", "How can I get to the bus stop?", "How can I get to...? পথ জিজ্ঞাসার কাজে লাগে।", "A1"),
            s("give-directions", "Give simple directions", "পথ বলে দেওয়া", "সোজা, ডানে ও বাঁয়ে যাওয়ার নির্দেশ দিতে পারবেন।", "সোজা যান।", "Go straight ahead.", "Go ahead straightly is.", "You goes straight.", "পরের রাস্তায় বাঁ দিকে ঘুরুন।", "Turn left at the next road.", "imperative-এ subject ছাড়া base verb দিয়ে শুরু করুন।", "A1"),
            s("transport-tickets", "Tickets and transport", "টিকিট ও যানবাহন", "টিকিট কিনতে ও গন্তব্য বলতে পারবেন।", "আমার ঢাকা যাওয়ার একটি টিকিট দরকার।", "I need a ticket to Dhaka.", "I need ticket at Dhaka.", "I am need a Dhaka ticket.", "পরের বাস কখন ছাড়ে?", "When does the next bus leave?", "গন্তব্যের আগে to ব্যবহার করুন।", "A1"),
            s("travel-problem", "Travel problems", "যাত্রার সমস্যা", "হারিয়ে যাওয়া, দেরি বা সাহায্যের প্রয়োজন জানাতে পারবেন।", "আমি পথ হারিয়েছি।", "I am lost.", "I have lose.", "I lost am road.", "আমার বাসটি দশ মিনিট দেরি করেছে।", "My bus is ten minutes late.", "lost ও late adjective-এর আগে be verb বসে।", "A1"),
        )),
        UnitSeed("health-safety", "Health and safety", "স্বাস্থ্য ও নিরাপত্তা", listOf(
            s("body-parts", "Parts of the body", "শরীরের অংশ", "শরীরের পরিচিত অংশ দেখিয়ে বলতে পারবেন।", "এটি আমার ডান হাত।", "This is my right hand.", "These is my right hand.", "This are hand right.", "আমার পায়ে ব্যথা করছে।", "My leg hurts.", "একটি অঙ্গের সমস্যায় My ... hurts বলা যায়।", "A1"),
            s("symptoms", "Describe symptoms", "অসুস্থতার লক্ষণ", "সাধারণ অসুস্থতা সহজভাবে বলতে পারবেন।", "আমার মাথা ব্যথা করছে।", "I have a headache.", "I am a headache.", "I has headache.", "আমার জ্বর ও কাশি আছে।", "I have a fever and a cough.", "symptom-এর সঙ্গে have ব্যবহার করা হয়।", "A1"),
            s("doctor-visit", "At the doctor", "ডাক্তারের কাছে", "সমস্যা কতদিন ধরে তা বলতে ও প্রশ্ন বুঝতে পারবেন।", "দুই দিন ধরে আমার শরীর খারাপ।", "I have felt ill for two days.", "I am ill since two days.", "I feel illness two days for.", "আমার কি ওষুধ দরকার?", "Do I need medicine?", "সময়ের দৈর্ঘ্যের আগে for ব্যবহার করুন।", "A1"),
            s("simple-advice", "Should and should not", "সহজ পরামর্শ", "should দিয়ে স্বাস্থ্য পরামর্শ দিতে পারবেন।", "আপনার বিশ্রাম নেওয়া উচিত।", "You should rest.", "You should to rest.", "You are should resting.", "আপনার ঠান্ডা পানি খাওয়া উচিত নয়।", "You should not drink cold water.", "should-এর পরে verb-এর base form থাকে।", "A1"),
            s("medicine", "Medicine instructions", "ওষুধের নির্দেশ", "সহজ dosage ও medicine instruction বুঝতে পারবেন।", "খাবারের পরে এই ওষুধটি নিন।", "Take this medicine after food.", "Takes this medicine after food.", "You taking medicine food.", "দিনে দুবার একটি tablet নিন।", "Take one tablet twice a day.", "instruction সাধারণত base verb দিয়ে শুরু হয়।", "A1"),
            s("emergency", "Get help in an emergency", "জরুরি সাহায্য", "জরুরি অবস্থায় দ্রুত সাহায্য চাইতে পারবেন।", "দয়া করে একটি ambulance ডাকুন।", "Please call an ambulance.", "Please calling ambulance.", "Call please a ambulance.", "আমার এখনই সাহায্য দরকার।", "I need help now.", "জরুরি বাক্য ছোট, স্পষ্ট ও সরাসরি রাখুন।", "A1"),
        )),
        UnitSeed("work-services", "Work and services", "কাজ ও সেবা", listOf(
            s("jobs-workplaces", "Jobs and workplaces", "পেশা ও কর্মস্থল", "নিজের কাজ ও কর্মস্থল বলতে পারবেন।", "আমি একটি দোকানে কাজ করি।", "I work in a shop.", "I am work at shop.", "I works in a shop.", "আমার বোন একজন nurse।", "My sister is a nurse.", "পেশার আগে সাধারণত a/an ব্যবহার করুন।", "A1"),
            s("abilities-can", "Can and cannot", "পারি ও পারি না", "নিজের দক্ষতা ও সীমাবদ্ধতা বলতে পারবেন।", "আমি computer ব্যবহার করতে পারি।", "I can use a computer.", "I can to use computer.", "I am can use computer.", "আমি গাড়ি চালাতে পারি না।", "I cannot drive.", "can/cannot-এর পরে base verb বসে।", "A1"),
            s("work-instructions", "Instructions at work", "কাজের নির্দেশ", "সহজ workplace instruction বুঝতে ও নিশ্চিত করতে পারবেন।", "দয়া করে দরজাটি বন্ধ করুন।", "Please close the door.", "Please closed the door.", "You closing door please.", "আমি কি এখানে sign করব?", "Should I sign here?", "নিশ্চিত হতে Should I...? জিজ্ঞাসা করা যায়।", "A1"),
            s("phone-call", "Make a phone call", "ফোনে কথা বলা", "পরিচয় দিয়ে কাউকে ফোনে চাইতে পারবেন।", "আমি কি মিস্টার দাসের সঙ্গে কথা বলতে পারি?", "May I speak to Mr Das?", "I may speaking Mr Das?", "Can speak I to Das?", "আমি একটি বার্তা রাখতে চাই।", "I would like to leave a message.", "ফোনে May I speak to...? একটি ভদ্র বাক্য।", "A1"),
            s("forms-details", "Fill in a simple form", "সহজ form পূরণ", "নাম, ঠিকানা, nationality ও contact detail দিতে পারবেন।", "এটি আমার পুরো নাম।", "This is my full name.", "These are my full name.", "This my name full.", "আমার ঠিকানা ১২ লেক রোড।", "My address is 12 Lake Road.", "ঠিকানা বলার জন্য My address is... ব্যবহার করুন।", "A1"),
            s("appointments", "Make an appointment", "সময় নির্ধারণ", "appointment চাওয়া, সময় প্রস্তাব ও পরিবর্তন করতে পারবেন।", "আমি একটি appointment নিতে চাই।", "I would like to make an appointment.", "I like make an appointment.", "I would making appointment.", "সোমবার সকাল দশটা কি ঠিক হবে?", "Is Monday at ten okay?", "তারিখ/দিন ও সময় স্পষ্টভাবে নিশ্চিত করুন।", "A1"),
        )),
        UnitSeed("past-plans", "Past events and plans", "অতীত ও ভবিষ্যৎ পরিকল্পনা", listOf(
            s("was-were", "Was and were", "ছিলাম ও ছিলেন", "অতীতে অবস্থা was/were দিয়ে বলতে পারবেন।", "আমি গতকাল বাড়িতে ছিলাম।", "I was at home yesterday.", "I were at home yesterday.", "I am home yesterday.", "তারা খুব ব্যস্ত ছিল।", "They were very busy.", "I/he/she-এর সঙ্গে was; you/we/they-এর সঙ্গে were।", "A1"),
            s("regular-past", "Regular past verbs", "নিয়মিত past verb", "-ed verb দিয়ে গতকালের কাজ বলতে পারবেন।", "আমি গতকাল কাজ করেছি।", "I worked yesterday.", "I work yesterday.", "I did worked yesterday.", "আমরা সন্ধ্যায় television দেখেছি।", "We watched television in the evening.", "অনেক regular verb-এর past form-এ -ed যোগ হয়।", "A1"),
            s("past-checkpoint", "Common irregular verbs", "পরিচিত irregular past", "went, had, ate, drank-এর মতো past form ব্যবহার করতে পারবেন।", "আমি গতকাল বাজারে গিয়েছিলাম।", "I went to the market yesterday.", "I go to the market yesterday.", "I did went to market.", "সে সকালে চা খেয়েছিল।", "She drank tea in the morning.", "go→went, drink→drank; এগুলো আলাদা করে শিখুন।", "A1"),
            s("past-questions", "Questions about the past", "অতীত নিয়ে প্রশ্ন", "did দিয়ে গতকালের কাজ জিজ্ঞাসা করতে পারবেন।", "আপনি গতকাল কী করেছিলেন?", "What did you do yesterday?", "What do you did yesterday?", "What did you did yesterday?", "আপনি কি কাজটি শেষ করেছেন?", "Did you finish the work?", "did-এর পরে verb-এর base form থাকে।", "A1"),
            s("going-to", "Plans with going to", "ভবিষ্যৎ পরিকল্পনা", "আগে থেকে করা plan বলতে পারবেন।", "আমি আগামীকাল পড়তে যাচ্ছি।", "I am going to study tomorrow.", "I going study tomorrow.", "I am go to studied tomorrow.", "আমরা শুক্রবার ভ্রমণ করতে যাচ্ছি।", "We are going to travel on Friday.", "plan-এর জন্য be + going to + base verb ব্যবহার করুন।", "A1"),
            s("invitations", "Invite and respond", "আমন্ত্রণ ও উত্তর", "সহজ আমন্ত্রণ দিতে, গ্রহণ বা প্রত্যাখ্যান করতে পারবেন।", "আপনি কি আমাদের সঙ্গে আসতে চান?", "Would you like to come with us?", "Do you like come with us?", "Would you to coming us?", "দুঃখিত, আমি সেদিন ব্যস্ত।", "Sorry, I am busy that day.", "Would you like to...? ভদ্র আমন্ত্রণ।", "A1"),
        )),
        UnitSeed("a1-integration", "A1 integration", "A1 সমন্বিত ব্যবহার", listOf(
            s("read-notice", "Understand a short notice", "ছোট notice বোঝা", "সময়, স্থান ও নিষেধের সহজ notice বুঝতে পারবেন।", "দোকানটি আজ বন্ধ।", "The shop is closed today.", "The shop close today is.", "Shop are closed today.", "এখানে ধূমপান করবেন না।", "Do not smoke here.", "notice-এর ভাষা সাধারণত ছোট ও সরাসরি।", "A1"),
            s("read-message", "Understand a short message", "ছোট message বোঝা", "পরিচিত বিষয়ে সংক্ষিপ্ত message-এর মূল তথ্য বুঝতে পারবেন।", "আমি দশ মিনিট দেরি করব।", "I will be ten minutes late.", "I will ten minutes late be.", "I am late yesterday ten minutes.", "station-এর সামনে আমার সঙ্গে দেখা করুন।", "Meet me in front of the station.", "message-এ সময় ও meeting place খুঁজুন।", "A1"),
            s("write-message", "Write a short message", "ছোট message লেখা", "দেরি, স্থান বা plan জানিয়ে ছোট message লিখতে পারবেন।", "আমি এখন বাসে আছি।", "I am on the bus now.", "I in the bus now.", "I am bus now on.", "আমি সন্ধ্যা ছয়টায় পৌঁছাব।", "I will arrive at six in the evening.", "ছোট message-এ কে, কোথায়, কখন স্পষ্ট করুন।", "A1"),
            s("full-introduction", "Give a full introduction", "পূর্ণ পরিচয়", "৩০–৪৫ সেকেন্ডে নিজের পরিচয় ও দৈনন্দিন জীবন বলতে পারবেন।", "আমি নীলা এবং আমি ঢাকায় থাকি।", "I am Nila and I live in Dhaka.", "I is Nila and live Dhaka.", "I am Nila living at Dhaka is.", "আমি অবসর সময়ে গান শুনি।", "I listen to music in my free time.", "পরিচয়ে নাম, স্থান, কাজ ও পছন্দ যোগ করুন।", "A1"),
            s("solve-real-task", "Complete a real-life task", "বাস্তব কাজ সম্পন্ন", "দিকনির্দেশ, দাম ও clarification এক কথোপকথনে ব্যবহার করতে পারবেন।", "দয়া করে আমাকে ticket counter দেখাবেন?", "Could you show me the ticket counter, please?", "Could showing me ticket counter?", "You show could counter me?", "একটি ticket-এর দাম কত?", "How much is one ticket?", "কাজ শেষ করতে প্রয়োজনমতো follow-up question করুন।", "A1"),
            s("a1-final-checkpoint", "A1 final checkpoint", "A1 চূড়ান্ত পুনরাবৃত্তি", "পরিচিত দৈনন্দিন পরিস্থিতিতে সহজ তথ্য আদান-প্রদান করতে পারবেন।", "আমি গতকাল অসুস্থ ছিলাম, কিন্তু আজ ভালো আছি।", "I was ill yesterday, but I am fine today.", "I am ill yesterday but was fine today.", "I were sick and today fine.", "আগামীকাল আমি কাজে ফিরব।", "I will return to work tomorrow.", "past, present ও future time marker দেখে verb বেছে নিন।", "A1"),
        )),
    )
}
