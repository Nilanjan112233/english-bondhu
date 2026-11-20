package com.englishbondhu.app.live

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.englishbondhu.app.BuildConfig
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import okhttp3.OkHttpClient
import okhttp3.Dns
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

enum class LiveConnectionState { IDLE, CONNECTING, LISTENING, ERROR }

class GeminiLiveSession(
    private val onState: (LiveConnectionState, String) -> Unit,
    private val onTranscript: (speaker: String, text: String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        // Some mobile carriers advertise IPv6 routes that cannot complete a WebSocket
        // upgrade. Keep IPv6 as a fallback, but try the working IPv4 route first.
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> =
                Dns.SYSTEM.lookup(hostname).sortedBy { address -> if (address is Inet4Address) 0 else 1 }
        })
        .build()
    private val running = AtomicBoolean(false)
    private val modelSpeaking = AtomicBoolean(false)
    private var socket: WebSocket? = null
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private val resumeMicrophone = Runnable { modelSpeaking.set(false) }
    private val setupTimeout = Runnable {
        if (!running.getAndSet(false)) return@Runnable
        socket?.cancel()
        socket = null
        releaseAudio()
        emitState(
            LiveConnectionState.ERROR,
            "Live সংযোগে সময় বেশি লাগছে। mobile data/Wi-Fi বদলে আবার চেষ্টা করুন।",
        )
    }

    fun start() {
        if (running.getAndSet(true)) return
        modelSpeaking.set(false)
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            running.set(false)
            emitState(LiveConnectionState.ERROR, "এই APK-তে Gemini API key নেই।")
            return
        }
        emitState(LiveConnectionState.CONNECTING, "Gemini Live-এর সঙ্গে সংযোগ হচ্ছে…")
        val endpoint = "wss://generativelanguage.googleapis.com/ws/" +
            "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent" +
            "?key=${BuildConfig.GEMINI_API_KEY}"
        socket = client.newWebSocket(Request.Builder().url(endpoint).build(), listener)
        main.postDelayed(setupTimeout, 20_000)
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        main.removeCallbacks(setupTimeout)
        runCatching {
            socket?.send(JSONObject().put("realtimeInput", JSONObject().put("audioStreamEnd", true)).toString())
        }
        // Also releases the echo canceler, which the previous inline cleanup leaked.
        releaseAudio()
        socket?.close(1000, "Learner ended session")
        socket = null
        emitState(LiveConnectionState.IDLE, "লাইভ সেশন বন্ধ হয়েছে।")
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            emitState(LiveConnectionState.CONNECTING, "সংযোগ হয়েছে—Live প্রস্তুত হচ্ছে…")
            val instruction = """
                You are English Bondhu, a patient spoken-English teacher for a Bengali-speaking
                CEFR Pre-A1/A1 learner. The learner may speak Bengali in Bengali script, romanized
                Bengali such as "ami bhalo achi", or beginner English. Understand Bengali as Bengali;
                do not guess that it is another language.

                Follow this teaching loop for every learner turn:
                1. Listen to the complete learner turn before responding.
                2. If the learner speaks Bengali, first give the natural, simple English sentence that
                   expresses their meaning. Then explain it in one or two short Bengali sentences.
                   Ask the learner to repeat the English sentence, and stop speaking so they can answer.
                3. If the learner speaks incorrect English, say "Try this:" and give one corrected English
                   sentence slowly and clearly. Explain only the most important correction briefly in Bengali.
                   Then say "Please say:" followed by the corrected English sentence, and stop to listen.
                4. If the learner's English is correct, confirm it briefly. Give at most one short Bengali tip
                   when useful, then ask one easy English question that requires a spoken English answer.
                5. When the learner successfully repeats a correction, praise them briefly and continue with
                   the next small question instead of correcting the same point again.

                Always model English first and use Bengali for concise explanations. Use short sentences and
                speak English slowly, clearly, and naturally. Teach only one correction at a time. Never answer
                your own question or speak on the learner's behalf. After asking the learner to speak, stop and
                wait. If pronunciation is clearly difficult to understand, model only that word or short phrase
                slowly and ask for one repeat. Do not invent pronunciation problems or numerical scores. Never
                mock a Bengali accent. Keep each response brief enough for a beginner to remember and repeat.
            """.trimIndent()
            val setup = JSONObject().put("setup", JSONObject()
                .put("model", "models/${BuildConfig.GEMINI_LIVE_MODEL}")
                .put("generationConfig", JSONObject()
                    .put("responseModalities", org.json.JSONArray().put("AUDIO"))
                    .put("speechConfig", JSONObject().put("voiceConfig", JSONObject()
                        .put("prebuiltVoiceConfig", JSONObject().put("voiceName", "Kore")))))
                .put("inputAudioTranscription", JSONObject())
                .put("outputAudioTranscription", JSONObject())
                .put("systemInstruction", JSONObject().put("parts", org.json.JSONArray()
                    .put(JSONObject().put("text", instruction)))))
            webSocket.send(setup.toString())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching { handleMessage(JSONObject(text)) }
                .onFailure { emitState(LiveConnectionState.ERROR, "Live response বোঝা যায়নি।") }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            // Gemini Live currently returns JSON, including setupComplete, in binary
            // WebSocket frames. Treat the UTF-8 payload exactly like a text frame.
            onMessage(webSocket, bytes.utf8())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!running.getAndSet(false)) return
            main.removeCallbacks(setupTimeout)
            releaseAudio()
            emitState(LiveConnectionState.ERROR, "Live connection ব্যর্থ: ${t.message ?: "unknown error"}")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!running.getAndSet(false)) return
            main.removeCallbacks(setupTimeout)
            releaseAudio()
            emitState(LiveConnectionState.IDLE, "Live session শেষ হয়েছে।")
        }
    }

    private fun handleMessage(message: JSONObject) {
        if (message.has("setupComplete")) {
            main.removeCallbacks(setupTimeout)
            emitState(LiveConnectionState.LISTENING, "কথা বলুন—AI শুনছে এবং উত্তর দেবে।")
            startAudio()
            val firstTurn = JSONObject().put("clientContent", JSONObject()
                .put("turns", org.json.JSONArray().put(JSONObject()
                    .put("role", "user")
                    .put("parts", org.json.JSONArray().put(JSONObject()
                        .put("text", """
                            Start the first practice turn now. Greet the learner briefly in easy English.
                            Explain in one short Bengali sentence that you will help them practise and correct
                            spoken English. Then ask them to say in English: "My name is [their name]."
                            Stop speaking and wait for the learner.
                        """.trimIndent())))))
                .put("turnComplete", true))
            socket?.send(firstTurn.toString())
            return
        }
        val content = message.optJSONObject("serverContent") ?: return
        content.optJSONObject("inputTranscription")?.optString("text")
            ?.takeIf { it.isNotBlank() }?.let { emitTranscript("আপনি", it) }
        content.optJSONObject("outputTranscription")?.optString("text")
            ?.takeIf { it.isNotBlank() }?.let { emitTranscript("AI বন্ধু", it) }
        if (content.optBoolean("interrupted", false)) {
            main.removeCallbacks(resumeMicrophone)
            modelSpeaking.set(false)
            player?.runCatching { pause(); flush(); play() }
        }
        val parts = content.optJSONObject("modelTurn")?.optJSONArray("parts")
        if (parts != null) {
            val hasAudio = (0 until parts.length()).any { index ->
                parts.optJSONObject(index)?.optJSONObject("inlineData")?.optString("data")?.isNotBlank() == true
            }
            if (hasAudio) {
                main.removeCallbacks(resumeMicrophone)
                modelSpeaking.set(true)
            }
            for (index in 0 until parts.length()) {
                val inline = parts.optJSONObject(index)?.optJSONObject("inlineData") ?: continue
                val encoded = inline.optString("data")
                if (encoded.isNotBlank()) playAudio(Base64.decode(encoded, Base64.DEFAULT))
            }
        }
        if (content.optBoolean("turnComplete", false)) {
            // Let the speaker buffer drain before forwarding microphone audio again.
            main.removeCallbacks(resumeMicrophone)
            main.postDelayed(resumeMicrophone, 900)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudio() {
        if (!running.get() || recorder != null) return
        val recordBuffer = maxOf(
            AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT),
            4_096,
        )
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            16_000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recordBuffer * 2,
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            // Microphone busy or unavailable: end the session cleanly instead of failing later.
            audioRecord.release()
            failSession("মাইক্রোফোন চালু করা যায়নি। অন্য app মাইক্রোফোন ব্যবহার করছে কি না দেখুন।")
            return
        }
        recorder = audioRecord
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(audioRecord.audioSessionId)?.apply { enabled = true }
        }
        audioRecord.startRecording()
        player = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(24_000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(24_000)
            .build().also { it.play() }

        thread(name = "gemini-live-microphone", isDaemon = true) {
            val samples = ShortArray(2_048)
            while (running.get()) {
                val count = recorder?.read(samples, 0, samples.size) ?: break
                if (count <= 0) continue
                // Continue draining AudioRecord but do not send the AI's own speaker
                // output back to Gemini as if it were the learner's voice.
                if (modelSpeaking.get()) continue
                val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
                repeat(count) { bytes.putShort(samples[it]) }
                val audio = JSONObject()
                    .put("mimeType", "audio/pcm;rate=16000")
                    .put("data", Base64.encodeToString(bytes.array(), Base64.NO_WRAP))
                socket?.send(JSONObject().put("realtimeInput", JSONObject().put("audio", audio)).toString())
            }
        }
    }

    private fun playAudio(bytes: ByteArray) {
        if (running.get()) player?.write(bytes, 0, bytes.size, AudioTrack.WRITE_BLOCKING)
    }

    private fun releaseAudio() {
        main.removeCallbacks(resumeMicrophone)
        modelSpeaking.set(false)
        echoCanceler?.release()
        echoCanceler = null
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        player?.runCatching { stop() }
        player?.release()
        player = null
    }

    private fun failSession(message: String) {
        if (!running.getAndSet(false)) return
        main.removeCallbacks(setupTimeout)
        releaseAudio()
        socket?.close(1000, "Client audio unavailable")
        socket = null
        emitState(LiveConnectionState.ERROR, message)
    }

    private fun emitState(state: LiveConnectionState, message: String) =
        main.post { onState(state, message) }

    private fun emitTranscript(speaker: String, text: String) =
        main.post { onTranscript(speaker, text) }
}
