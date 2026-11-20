package com.englishbondhu.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.englishbondhu.app.ai.NetworkAiTutor
import com.englishbondhu.app.ai.TutorMessage
import com.englishbondhu.app.data.PracticeContentRepository
import com.englishbondhu.app.data.ProgressStore
import com.englishbondhu.app.data.SampleCourseRepository
import com.englishbondhu.app.domain.AnswerChecker
import com.englishbondhu.app.domain.LearningStats
import com.englishbondhu.app.domain.PlacementResult
import com.englishbondhu.app.live.GeminiLiveSession
import com.englishbondhu.app.live.LiveConnectionState
import com.englishbondhu.app.model.Exercise
import com.englishbondhu.app.model.ExerciseType
import com.englishbondhu.app.model.LearningUnit
import com.englishbondhu.app.model.Lesson
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.launch

private enum class Destination(val icon: String, val label: String) {
    HOME("⌂", "হোম"), LEARN("▤", "শিখুন"), PRACTICE("◎", "প্র্যাকটিস"),
    TUTOR("✦", "AI বন্ধু"), LIVE("◉", "লাইভ"),
}

private const val BEGINNER_SPEECH_RATE = 0.72f

private fun prepareBeginnerEnglishVoice(engine: TextToSpeech): Boolean {
    val languageStatus = engine.setLanguage(Locale.US)
    engine.setSpeechRate(BEGINNER_SPEECH_RATE)
    engine.setPitch(1.0f)
    return languageStatus != TextToSpeech.LANG_MISSING_DATA &&
        languageStatus != TextToSpeech.LANG_NOT_SUPPORTED
}

private data class PracticeOption(val icon: String, val title: String, val description: String, val lesson: Lesson)
private enum class SessionKind { LESSON, PRACTICE, PLACEMENT }
private data class ActiveSession(val lesson: Lesson, val kind: SessionKind)
private data class SessionResult(val correct: Int, val total: Int)

@Composable
fun EnglishBondhuApp() {
    val context = LocalContext.current
    val course = remember { SampleCourseRepository().activeCourse() }
    val allLessons = remember(course) { course.units.flatMap { it.lessons } }
    val progressStore = remember { ProgressStore(context.applicationContext) }
    var destination by remember { mutableStateOf(Destination.HOME) }
    var activeSession by remember { mutableStateOf<ActiveSession?>(null) }
    var completedIds by remember { mutableStateOf(progressStore.completedLessonIds()) }
    var learningStats by remember { mutableStateOf(progressStore.learningStats()) }
    var placementResult by remember { mutableStateOf(progressStore.placementResult()) }
    var showWelcome by remember { mutableStateOf(!progressStore.hasSeenWelcome()) }
    val placementLesson = remember(allLessons) { PracticeContentRepository.placementTest(allLessons.flatMap { it.exercises }) }
    val lastCompletedIndex = allLessons.indexOfFirst { it.id == progressStore.lastCompletedLessonId() }
    val nextLesson = when {
        completedIds.isEmpty() && placementResult != null ->
            allLessons[placementResult!!.recommendedLessonIndex.coerceIn(allLessons.indices)]
        lastCompletedIndex >= 0 ->
            allLessons.drop(lastCompletedIndex + 1).firstOrNull { it.id !in completedIds }
                ?: allLessons.firstOrNull { it.id !in completedIds } ?: allLessons.last()
        else -> allLessons.firstOrNull { it.id !in completedIds } ?: allLessons.last()
    }

    if (showWelcome && activeSession == null) {
        WelcomeScreen(
            onPlacement = {
                progressStore.markWelcomeSeen()
                showWelcome = false
                activeSession = ActiveSession(placementLesson, SessionKind.PLACEMENT)
            },
            onStartBeginning = {
                progressStore.markWelcomeSeen()
                showWelcome = false
                activeSession = ActiveSession(allLessons.first(), SessionKind.LESSON)
            },
            onBrowse = {
                progressStore.markWelcomeSeen()
                showWelcome = false
                destination = Destination.LEARN
            },
        )
        return
    }

    if (activeSession != null) {
        val session = activeSession!!
        LessonSession(
            lesson = session.lesson,
            onClose = { activeSession = null },
            onAttempt = { exercise, answer, correct ->
                if (session.kind != SessionKind.PLACEMENT) {
                    progressStore.recordExerciseAttempt(exercise.id, answer, correct)
                    learningStats = progressStore.learningStats()
                }
            },
            onComplete = { result ->
                when (session.kind) {
                    SessionKind.LESSON -> {
                        progressStore.markLessonCompleted(session.lesson.id)
                        completedIds = progressStore.completedLessonIds()
                    }
                    SessionKind.PLACEMENT -> {
                        progressStore.savePlacement(result.correct, result.total)
                        placementResult = progressStore.placementResult()
                    }
                    SessionKind.PRACTICE -> Unit
                }
                learningStats = progressStore.learningStats()
                activeSession = null
            },
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Text(item.icon, fontSize = 22.sp) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (destination) {
            Destination.HOME -> HomeScreen(
                completedLessons = completedIds.count { id -> allLessons.any { it.id == id } },
                streak = progressStore.streak(),
                totalXp = allLessons.filter { it.id in completedIds }.sumOf { it.xp },
                nextLesson = nextLesson,
                stats = learningStats,
                placement = placementResult,
                onStart = { activeSession = ActiveSession(it, SessionKind.LESSON) },
                onPlacement = { activeSession = ActiveSession(placementLesson, SessionKind.PLACEMENT) },
                onTutor = { destination = Destination.TUTOR },
                modifier = Modifier.padding(padding),
            )
            Destination.LEARN -> CourseScreen(
                course.units, completedIds, nextLesson.id,
                { activeSession = ActiveSession(it, SessionKind.LESSON) }, Modifier.padding(padding),
            )
            Destination.PRACTICE -> PracticeScreen(
                lessons = allLessons,
                stats = learningStats,
                weakIds = progressStore.weakExerciseIds(),
                dueIds = progressStore.dueExerciseIds(),
                onPractice = { activeSession = ActiveSession(it, SessionKind.PRACTICE) },
                modifier = Modifier.padding(padding),
            )
            Destination.TUTOR -> TutorScreen(Modifier.padding(padding).consumeWindowInsets(padding).imePadding())
            Destination.LIVE -> LiveSpeakingScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun WelcomeScreen(
    onPlacement: () -> Unit,
    onStartBeginning: () -> Unit,
    onBrowse: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text("BN → EN", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Text("English Bondhu-তে স্বাগতম", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("বাংলা থেকে ধাপে ধাপে ব্যবহারিক English শিখুন।", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
        }
        item { WelcomeBenefit("১", "নিজের জায়গা থেকে শুরু", "Placement test নিন, প্রথম lesson থেকে শুরু করুন, অথবা যেকোনো unit নিজে বেছে নিন।") }
        item { WelcomeBenefit("২", "চারভাবে অনুশীলন", "শোনা, বলা, লেখা এবং সঠিক উত্তর বেছে নেওয়া—প্রতিটি lesson-এ।") }
        item { WelcomeBenefit("৩", "AI-এর সঙ্গে কথা", "বাংলায় প্রশ্ন করুন বা Live-এ English বলুন। AI ব্যবহার করতে internet লাগবে।") }
        item {
            Button(onClick = onPlacement, Modifier.fillMaxWidth().height(56.dp)) {
                Text("আমার level যাচাই করুন", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onStartBeginning, Modifier.fillMaxWidth().height(52.dp)) {
                Text("একদম শুরু থেকে শিখব")
            }
            TextButton(onClick = onBrowse, Modifier.fillMaxWidth()) {
                Text("সব lesson দেখে নিজে বেছে নেব")
            }
            Text("কোনো lesson locked নয়—পরে যেকোনো সময় পথ বদলাতে পারবেন।", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WelcomeBenefit(number: String, title: String, description: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(number, Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun LiveSpeakingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(LiveConnectionState.IDLE) }
    var status by remember { mutableStateOf("AI-এর সঙ্গে সরাসরি ইংরেজিতে কথা বলুন।") }
    val transcript = remember { mutableStateListOf<Pair<String, String>>() }
    val transcriptState = rememberLazyListState()
    val session = remember {
        GeminiLiveSession(
            onState = { next, message -> state = next; status = message },
            onTranscript = { speaker, text ->
                if (transcript.lastOrNull()?.first == speaker) {
                    val previous = transcript.removeAt(transcript.lastIndex)
                    transcript += speaker to (previous.second + text)
                } else transcript += speaker to text
            },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) session.start() else status = "লাইভ কথোপকথনের জন্য microphone permission দরকার।"
    }
    DisposableEffect(session) { onDispose { session.stop() } }
    LaunchedEffect(transcript.size) {
        if (transcript.isNotEmpty()) transcriptState.animateScrollToItem(transcript.lastIndex)
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(20.dp)) {
            Text("◉ Gemini Live speaking", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("বাংলা বুঝবে • সহজ English বলবে • ভুল ঠিক করবে", color = Color.White.copy(alpha = .82f))
        }
        Card(
            Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state == LiveConnectionState.LISTENING) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (state == LiveConnectionState.LISTENING) "●" else "○", color = MaterialTheme.colorScheme.primary, fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Text(status, Modifier.weight(1f), fontWeight = FontWeight.Bold)
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = transcriptState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (transcript.isEmpty()) item {
                Text(
                    "Start চাপুন। AI প্রথমে সহজ English-এ পরিচয় জিজ্ঞাসা করবে। আপনি বাংলা বা English—দুই ভাষাতেই কথা বলতে পারবেন।",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
            items(transcript) { line ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (line.first == "আপনি") MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(line.first, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(line.second)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    if (state == LiveConnectionState.LISTENING || state == LiveConnectionState.CONNECTING) {
                        session.stop()
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        session.start()
                    } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = if (state == LiveConnectionState.LISTENING) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.buttonColors(),
            ) {
                Text(
                    when (state) {
                        LiveConnectionState.CONNECTING -> "× সংযোগ বাতিল করুন"
                        LiveConnectionState.LISTENING -> "■ সেশন বন্ধ করুন"
                        else -> "🎙 Live conversation শুরু করুন"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
            if (transcript.isNotEmpty() && state != LiveConnectionState.LISTENING && state != LiveConnectionState.CONNECTING) {
                TextButton(onClick = { transcript.clear() }) { Text("পুরোনো transcript মুছুন") }
            }
            Text(
                "কথার audio Gemini-তে পাঠানো হয়। একটি session সর্বোচ্চ ১৫ মিনিট; headphone ব্যবহার করলে echo কম হবে।",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    completedLessons: Int,
    streak: Int,
    totalXp: Int,
    nextLesson: Lesson,
    stats: LearningStats,
    placement: PlacementResult?,
    onStart: (Lesson) -> Unit,
    onPlacement: () -> Unit,
    onTutor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "সুপ্রভাত!"
        in 12..16 -> "শুভ অপরাহ্ণ!"
        else -> "শুভ সন্ধ্যা!"
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("আজ ১০ মিনিট ইংরেজি বলি", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("BN → EN", Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("আপনার জন্য পরের পরামর্শ", color = Color.White.copy(alpha = .8f))
                    Text("${nextLesson.titleBn}\n${nextLesson.titleEn}", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("◷ ${nextLesson.minutes} মিনিট", color = Color.White)
                        Text("★ ${nextLesson.xp} XP", color = Color.White)
                    }
                    Button(
                        onClick = { onStart(nextLesson) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("শেখা শুরু করুন", fontWeight = FontWeight.Bold) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("🔥", "$streak দিন", "স্ট্রিক", Modifier.weight(1f))
                StatCard("✓", "$completedLessons", "লেসন শেষ", Modifier.weight(1f))
                StatCard("★", "$totalXp", "মোট XP", Modifier.weight(1f))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onPlacement),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (placement == null) "আপনার স্তর জানুন" else "বর্তমান স্তর: ${placement.levelBn}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        if (placement == null) "১০ মিনিটের placement test দিয়ে সঠিক লেসন থেকে শুরু করুন।"
                        else "${placement.correct}/${placement.total} সঠিক • ${placement.level} • আবার পরীক্ষা দিতে চাপুন",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("আপনার শেখার অগ্রগতি", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${stats.attempts} উত্তর • ${stats.accuracyPercent}% accuracy • ${stats.weakItems}টি দুর্বল বিষয়")
                    LinearProgressIndicator(
                        progress = { stats.accuracyPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    )
                    Text(
                        if (stats.dueReviews == 0) "আজ কোনো review বাকি নেই ✓" else "আজ review বাকি: ${stats.dueReviews}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onTutor),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", fontSize = 32.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AI ইংরেজি বন্ধু", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("বাংলায় প্রশ্ন করুন • ইংরেজিতে প্র্যাকটিস করুন")
                    }
                    Text("›", fontSize = 28.sp)
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: String, value: String, label: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 21.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CourseScreen(
    units: List<LearningUnit>,
    completedIds: Set<String>,
    recommendedLessonId: String,
    onLesson: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lessons = units.flatMap { it.lessons }
    val recommendedUnitId = units.firstOrNull { unit -> unit.lessons.any { it.id == recommendedLessonId } }?.id
    var expandedUnits by remember(recommendedUnitId) { mutableStateOf(setOfNotNull(recommendedUnitId)) }
    var query by remember { mutableStateOf("") }
    var onlyIncomplete by remember { mutableStateOf(false) }
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    val visibleUnits = units.mapNotNull { unit ->
        val unitMatches = normalizedQuery.isBlank() ||
            unit.titleBn.contains(normalizedQuery, ignoreCase = true) || unit.titleEn.contains(normalizedQuery, ignoreCase = true)
        val visibleLessons = unit.lessons.filter { lesson ->
            val matchesText = unitMatches || lesson.titleBn.contains(normalizedQuery, ignoreCase = true) ||
                lesson.titleEn.contains(normalizedQuery, ignoreCase = true) || lesson.canDoBn.contains(normalizedQuery, ignoreCase = true)
            matchesText && (!onlyIncomplete || lesson.id !in completedIds)
        }
        unit.takeIf { visibleLessons.isNotEmpty() }?.copy(lessons = visibleLessons)
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("শেখার পথ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Pre-A1 → A1 • ১২টি unit • ৭২টি lesson", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("সব lesson খোলা—যেখান থেকে স্বচ্ছন্দ, সেখান থেকেই শুরু করুন। ★ শুধু পরামর্শ।", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { completedIds.count { id -> lessons.any { it.id == id } } / lessons.size.toFloat() }, modifier = Modifier.fillMaxWidth().clip(CircleShape))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("lesson বা topic খুঁজুন") },
                placeholder = { Text("যেমন: shopping, সময়, health") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onlyIncomplete = false }, Modifier.weight(1f)) {
                    Text(if (!onlyIncomplete) "✓ সব lesson" else "সব lesson")
                }
                OutlinedButton(onClick = { onlyIncomplete = true }, Modifier.weight(1f)) {
                    Text(if (onlyIncomplete) "✓ শুধু অসমাপ্ত" else "শুধু অসমাপ্ত")
                }
            }
        }
        if (visibleUnits.isEmpty()) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("কোনো lesson পাওয়া যায়নি", fontWeight = FontWeight.Bold)
                    Text("অন্য শব্দ লিখুন বা ‘সব lesson’ নির্বাচন করুন।", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        visibleUnits.forEach { unit ->
            val unitIndex = units.indexOfFirst { it.id == unit.id }
            val sourceUnit = units[unitIndex]
            val expanded = normalizedQuery.isNotBlank() || unit.id in expandedUnits
            val completedInUnit = sourceUnit.lessons.count { it.id in completedIds }
            item(key = "header-${unit.id}") {
                Card(
                    Modifier.fillMaxWidth().clickable {
                        expandedUnits = if (expanded) expandedUnits - unit.id else expandedUnits + unit.id
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${unitIndex + 1}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(unit.titleBn, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${unit.titleEn} • $completedInUnit/${sourceUnit.lessons.size} শেষ", fontSize = 13.sp)
                        }
                        Text(if (expanded) "⌃" else "⌄", fontSize = 24.sp)
                    }
                }
            }
            if (expanded) items(unit.lessons, key = { it.id }) { lesson ->
                val completed = lesson.id in completedIds
                val recommended = lesson.id == recommendedLessonId
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onLesson(lesson) },
                    shape = RoundedCornerShape(20.dp),
                    colors = if (recommended) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(if (completed) "✓" else if (recommended) "★" else "▶", Modifier.padding(15.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(lesson.titleBn, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${lesson.titleEn} • ${lesson.cefrLevel}", color = MaterialTheme.colorScheme.primary)
                            Text(lesson.canDoBn, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${lesson.minutes}m", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeScreen(
    lessons: List<Lesson>,
    stats: LearningStats,
    weakIds: Set<String>,
    dueIds: Set<String>,
    onPractice: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises = remember(lessons) { lessons.flatMap { it.exercises } }
    val weakExercises = remember(exercises, weakIds) { exercises.filter { it.id in weakIds }.take(3) }
    val prioritizedReview = remember(exercises, weakIds, dueIds) {
        (exercises.filter { it.id in dueIds } + exercises.filter { it.id in weakIds } + exercises)
            .distinctBy { it.id }
            .take(10)
    }
    val options = remember(exercises, prioritizedReview, stats) { listOf(
        PracticeOption("🎙", "উচ্চারণ", "বাক্য শুনুন, বলুন এবং transcript মিলান",
            Lesson("practice-speaking", "Speaking practice", "উচ্চারণ অনুশীলন", "English speaking", 5, 10,
                exercises.filter { it.type == ExerciseType.SPEAKING }.take(8))),
        PracticeOption("🎧", "শোনার অনুশীলন", "English শুনে বাংলা অর্থ বুঝুন",
            Lesson("practice-listening", "Listening practice", "শোনার অনুশীলন", "Listening", 5, 10,
                exercises.filter { it.type == ExerciseType.LISTENING }.take(8))),
        PracticeOption("▣", "আজকের smart review",
            if (stats.attempts == 0) "Lesson করার পরে আপনার দুর্বল উত্তরগুলো এখানে আগে আসবে"
            else "${stats.dueReviews}টি due • ${stats.weakItems}টি দুর্বল বিষয় আগে আসবে",
            Lesson("practice-review", "Review", "শব্দ ও বাক্য রিভিউ", "Review", 7, 15,
                prioritizedReview)),
        PracticeOption("✎", "দ্রুত পরীক্ষা", "১২টি mixed A1 প্রশ্ন",
            Lesson("practice-test", "Quick test", "দ্রুত A1 পরীক্ষা", "Mixed test", 10, 20,
                exercises.shuffled().take(12))),
    ) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("প্র্যাকটিস", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("যেখানে দুর্বল, সেখানেই বেশি অনুশীলন", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (weakExercises.isNotEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("আবার দেখার বিষয়", fontWeight = FontWeight.Bold)
                    weakExercises.forEach { weak -> Text("• ${weak.prompt}", fontSize = 14.sp) }
                    Text("Smart Review খুললে এগুলো আগে আসবে।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        items(options) { item ->
            Card(Modifier.fillMaxWidth().clickable { onPractice(item.lesson) }, shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.icon, fontSize = 30.sp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Text("বাস্তব কথোপকথন", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text("পরিস্থিতি দেখে নিজের উত্তর English-এ লিখুন", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(PracticeContentRepository.dialogueScenarios()) { scenario ->
            Card(Modifier.fillMaxWidth().clickable { onPractice(scenario) }, shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💬", fontSize = 28.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(scenario.titleBn, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(scenario.titleEn, color = MaterialTheme.colorScheme.primary)
                        Text(scenario.descriptionBn, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                    Text("›", fontSize = 28.sp)
                }
            }
        }
    }
}

@Composable
private fun LessonSession(
    lesson: Lesson,
    onClose: () -> Unit,
    onAttempt: (Exercise, String, Boolean) -> Unit,
    onComplete: (SessionResult) -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var creditedCurrent by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    val exercise = lesson.exercises[index]
    val isCorrect = AnswerChecker.isCorrect(answer, exercise.answer)
    val result = SessionResult(correctCount, lesson.exercises.size)
    val finishOrAdvance = {
        if (index == lesson.exercises.lastIndex) {
            showSummary = true
        } else {
            index++
            answer = ""
            checked = false
            creditedCurrent = false
        }
    }
    val checkAnswer = {
        checked = true
        if (isCorrect && !creditedCurrent) {
            correctCount++
            creditedCurrent = true
        }
        onAttempt(exercise, answer, isCorrect)
    }
    val requestClose = {
        if (index > 0 || answer.isNotBlank() || checked) confirmExit = true else onClose()
    }

    BackHandler {
        if (showSummary) onComplete(result) else requestClose()
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("এখন lesson ছাড়বেন?") },
            text = { Text("এই session-এর অসম্পূর্ণ উত্তরগুলো lesson completion হিসেবে save হবে না। আগের saved progress থাকবে।") },
            confirmButton = { TextButton(onClick = onClose) { Text("বেরিয়ে যান") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("শেখা চালিয়ে যান") } },
        )
    }

    if (showSummary) {
        val percent = correctCount * 100 / lesson.exercises.size
        Scaffold { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(if (percent >= 75) "✓" else "↻", Modifier.padding(22.dp), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
                Text("Session সম্পন্ন", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(lesson.titleBn, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Text("$correctCount/${lesson.exercises.size} সঠিক • $percent%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    when {
                        percent >= 90 -> "দারুণ! এই বিষয়টি আপনার বেশ ভালো হয়েছে।"
                        percent >= 60 -> "ভালো হয়েছে। ভুলগুলো Smart Review-এ আবার আসবে।"
                        else -> "শুরুটা হয়েছে। উত্তরগুলো দেখে আরেকবার practice করলে সহজ হবে।"
                    },
                    Modifier.padding(top = 10.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))
                Button(onClick = { onComplete(result) }, Modifier.fillMaxWidth().height(56.dp)) {
                    Text("ফল save করে ফিরে যান", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Scaffold { padding ->
        Column(
            // imePadding keeps the check button above the keyboard on typed-answer questions.
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("×", Modifier.clickable(onClick = requestClose).padding(8.dp), fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                LinearProgressIndicator(
                    progress = { (index + 1f) / lesson.exercises.size },
                    modifier = Modifier.weight(1f).height(10.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Text("${index + 1}/${lesson.exercises.size}", fontWeight = FontWeight.Bold)
            }
            Text("${lesson.titleBn} • ${lesson.cefrLevel}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(exercise.instructionBn, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ExercisePrompt(
                exercise = exercise,
                answer = answer,
                checked = checked,
                onAnswer = { if (!checked) answer = it },
                onSubmit = { if (!checked && answer.isNotBlank()) checkAnswer() },
            )
            Spacer(Modifier.weight(1f))
            if (checked) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(if (isCorrect) "সঠিক! দারুণ করেছেন ✓" else "ভুল থেকে শেখাই আসল practice", fontWeight = FontWeight.Bold)
                        if (!isCorrect) Text("সঠিক উত্তর: ${exercise.answer}")
                        Text(exercise.explanationBn, fontSize = 14.sp)
                    }
                }
            }
            if (checked && !isCorrect) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { answer = ""; checked = false },
                        modifier = Modifier.weight(1f).height(54.dp),
                    ) { Text("আবার চেষ্টা") }
                    Button(onClick = finishOrAdvance, modifier = Modifier.weight(1f).height(54.dp)) {
                        Text(if (index == lesson.exercises.lastIndex) "ফল দেখুন" else "এগিয়ে যান", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    enabled = answer.isNotBlank(),
                    onClick = { if (!checked) checkAnswer() else finishOrAdvance() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Text(
                        when {
                            !checked -> "উত্তর যাচাই করুন"
                            index == lesson.exercises.lastIndex -> "ফল দেখুন • $correctCount/${lesson.exercises.size}"
                            else -> "পরের প্রশ্ন"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisePrompt(
    exercise: Exercise,
    answer: String,
    checked: Boolean,
    onAnswer: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PromptCard(
            text = exercise.prompt,
            speakAutomatically = exercise.type == ExerciseType.LISTENING,
            concealText = exercise.type == ExerciseType.LISTENING && !checked,
        )
        when (exercise.type) {
            ExerciseType.CHOICE, ExerciseType.LISTENING -> exercise.choices.forEach { choice ->
                OutlinedButton(
                    onClick = { onAnswer(choice) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (answer == choice) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else ButtonDefaults.outlinedButtonColors(),
                ) { Text(choice, Modifier.padding(6.dp), fontSize = 17.sp) }
            }
            ExerciseType.TRANSLATION -> OutlinedTextField(
                value = answer,
                onValueChange = onAnswer,
                label = { Text("ইংরেজিতে লিখুন") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )
            ExerciseType.SPEAKING -> SpeakingInput(exercise.prompt, answer, onAnswer)
        }
    }
}

@Composable
private fun PromptCard(text: String, speakAutomatically: Boolean, concealText: Boolean = false) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = prepareBeginnerEnglishVoice(engine)
            }
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    LaunchedEffect(text, speakAutomatically, ttsReady) {
        if (speakAutomatically && ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lesson-prompt")
    }
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (concealText) "🔊 অডিও শুনুন\nলেখা উত্তর দেওয়ার পরে দেখা যাবে" else text,
                Modifier.weight(1f),
                fontSize = if (concealText) 18.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (text.any { it in 'A'..'Z' || it in 'a'..'z' }) {
                Column(
                    Modifier.clickable(enabled = ttsReady) {
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "manual-prompt")
                    }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🔊", fontSize = 25.sp)
                    Text("ধীরে শুনুন", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SpeakingInput(prompt: String, answer: String, onAnswer: (String) -> Unit) {
    val context = LocalContext.current
    var hint by remember { mutableStateOf("মাইক্রোফোন চাপুন, তারপর বাক্যটি বলুন") }
    val intent = remember(prompt) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
    }
    val recognitionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!matches.isNullOrEmpty()) {
            onAnswer(matches.first())
            hint = "শোনা হয়েছে: ${matches.first()}"
        } else hint = "কথা বোঝা যায়নি—আবার চেষ্টা করুন"
    }
    val noRecognizer = "এই ডিভাইসে speech recognition service নেই"
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (!recognitionLauncher.launchSafely(intent)) hint = noRecognizer
        } else hint = "ভয়েস প্র্যাকটিসের জন্য microphone permission দরকার"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    hint = noRecognizer
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    if (!recognitionLauncher.launchSafely(intent)) hint = noRecognizer
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.size(76.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) { Text("🎙", fontSize = 30.sp) }
        Spacer(Modifier.height(8.dp))
        Text(hint, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (answer.isNotBlank()) Text(answer, Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun TutorScreen(modifier: Modifier = Modifier) {
    val tutor = remember { NetworkAiTutor() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) prepareBeginnerEnglishVoice(engine)
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    val messages = remember {
        mutableStateListOf(
            TutorMessage("নমস্কার! আমি আপনার ইংরেজি বন্ধু। বাংলায় প্রশ্ন করুন বা ইংরেজিতে কথা বলার অনুশীলন করুন।", false),
        )
    }
    var input by remember { mutableStateOf("") }
    var waiting by remember { mutableStateOf(false) }
    var micNotice by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val sendText: (String) -> Unit = { raw ->
        val sent = raw.trim()
        if (sent.isNotBlank() && !waiting) {
            input = ""
            messages += TutorMessage(sent, true)
            waiting = true
            scope.launch {
                messages += TutorMessage(tutor.reply(sent, "bn", "A1"), false)
                waiting = false
            }
        }
    }
    LaunchedEffect(messages.size, waiting) {
        val target = if (waiting) messages.size else messages.lastIndex
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.background(MaterialTheme.colorScheme.primary).fillMaxWidth().padding(20.dp)) {
            Text("✦ AI ইংরেজি বন্ধু", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("বাংলা ব্যাখ্যা • A1 স্তর", color = Color.White.copy(alpha = .8f))
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages) { message ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.fromLearner) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        color = if (message.fromLearner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (message.fromLearner) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(.84f),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(message.text, fontSize = 16.sp)
                            val spokenEnglish = message.text.lineSequence()
                                .filter { it.startsWith("Correct English:") || it.startsWith("Your turn:") }
                                .map { it.substringAfter(':').trim() }
                                .filter { it.isNotBlank() }
                                .joinToString(". ")
                            if (!message.fromLearner && spokenEnglish.isNotBlank()) {
                                Text(
                                    "🔊 ধীরে English শুনুন",
                                    Modifier.clickable {
                                        tts?.speak(spokenEnglish, TextToSpeech.QUEUE_FLUSH, null, "tutor-reply")
                                    }.padding(top = 8.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
            if (messages.size == 1) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("কীভাবে শুরু করবেন?", fontWeight = FontWeight.Bold)
                        listOf(
                            "আমাকে সহজ English-এ নিজের পরিচয় practice করাও।",
                            "I am go to market yesterday. বাক্যটি ঠিক করে বোঝাও।",
                            "দোকানে কেনাকাটার একটি ছোট role-play শুরু করো।",
                        ).forEach { prompt ->
                            OutlinedButton(onClick = { sendText(prompt) }, Modifier.fillMaxWidth()) { Text(prompt, textAlign = TextAlign.Start) }
                        }
                    }
                }
            }
            if (waiting) item { Text("বন্ধু ভাবছে…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TutorMicButton(onText = { input = it }, onUnavailable = { micNotice = it })
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("বাংলায় বা English-এ লিখুন…") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendText(input) }),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = input.isNotBlank() && !waiting,
                onClick = { sendText(input) },
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) { Text("➤") }
        }
        Text(
            micNotice ?: "Internet থাকলে Gemini উত্তর ও correction দেবে; connection না থাকলে basic offline practice চলবে।",
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A recognition *service* can exist without an activity that handles ACTION_RECOGNIZE_SPEECH
 * (common on emulators and de-Googled phones). Launching then throws, so report it instead.
 */
private fun ActivityResultLauncher<Intent>.launchSafely(intent: Intent): Boolean =
    try {
        launch(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

@Composable
private fun TutorMicButton(onText: (String) -> Unit, onUnavailable: (String) -> Unit) {
    val context = LocalContext.current
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak English. Your AI tutor will correct the transcript.")
        }
    }
    val recognitionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onText)
    }
    val noRecognizer = "এই ডিভাইসে speech recognition নেই—লিখে পাঠান।"
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) onUnavailable("ভয়েস ইনপুটের জন্য microphone permission দরকার")
        else if (!recognitionLauncher.launchSafely(speechIntent)) onUnavailable(noRecognizer)
    }

    OutlinedButton(
        onClick = {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onUnavailable(noRecognizer)
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                if (!recognitionLauncher.launchSafely(speechIntent)) onUnavailable(noRecognizer)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        modifier = Modifier.size(54.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) { Text("🎤", fontSize = 22.sp) }
}
