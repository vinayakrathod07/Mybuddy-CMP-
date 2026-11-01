package com.academialife.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.Locale

class AIChatFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnVoice: FrameLayout
    private lateinit var btnSend: FrameLayout
    private lateinit var btnAttachPhoto: FrameLayout

    private lateinit var adapter: ChatAdapter
    private var tts: TextToSpeech? = null
    private val messagesList = mutableListOf<ChatMessage>()

    private var currentLanguage = "English"

    private lateinit var chipLangEnglish: TextView
    private lateinit var chipLangHindi: TextView
    private lateinit var chipLangHinglish: TextView
    private lateinit var chipLangKannada: TextView
    private lateinit var chipLangTelugu: TextView
    private lateinit var chipLangTamil: TextView
    private lateinit var chipLangMarathi: TextView
    private lateinit var chipLangSpanish: TextView

    // Gallery Picker for Timetable/Attendance Photos
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            processSelectedImage(uri)
        }
    }

    // Camera Capture Launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            analyzeBitmapWithAI(bitmap)
        }
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                etMessage.setText(spokenText)
                sendMessage(spokenText)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_ai_chat, container, false)
        rvMessages = root.findViewById(R.id.rvChatMessages)
        etMessage = root.findViewById(R.id.etChatMessage)
        btnVoice = root.findViewById(R.id.btnVoiceInput)
        btnSend = root.findViewById(R.id.btnSendMessage)
        btnAttachPhoto = root.findViewById(R.id.btnAttachPhoto)

        chipLangEnglish = root.findViewById(R.id.chipLangEnglish)
        chipLangHindi = root.findViewById(R.id.chipLangHindi)
        chipLangHinglish = root.findViewById(R.id.chipLangHinglish)
        chipLangKannada = root.findViewById(R.id.chipLangKannada)
        chipLangTelugu = root.findViewById(R.id.chipLangTelugu)
        chipLangTamil = root.findViewById(R.id.chipLangTamil)
        chipLangMarathi = root.findViewById(R.id.chipLangMarathi)
        chipLangSpanish = root.findViewById(R.id.chipLangSpanish)

        setupLanguageChips()

        tts = TextToSpeech(requireContext(), this)

        adapter = ChatAdapter(messagesList) { textToSpeak ->
            speakTextFriendly(textToSpeak)
        }

        rvMessages.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        // Setup Quick Prompts
        root.findViewById<TextView>(R.id.promptTodayClasses)?.setOnClickListener {
            sendMessage("What classes do I have scheduled for today?")
        }

        root.findViewById<TextView>(R.id.promptAttendance)?.setOnClickListener {
            sendMessage("Can you summarize my attendance percentage and compliance risk?")
        }

        root.findViewById<TextView>(R.id.promptUrgentTasks)?.setOnClickListener {
            sendMessage("What are my top priority tasks and exam deadlines?")
        }

        root.findViewById<TextView>(R.id.promptStudyPlan)?.setOnClickListener {
            sendMessage("What study sessions do I have planned or need to schedule?")
        }

        root.findViewById<TextView>(R.id.promptWater)?.setOnClickListener {
            sendMessage("Log 1 glass of water for me")
        }

        root.findViewById<TextView>(R.id.promptStrategy)?.setOnClickListener {
            sendMessage("What is my proactive academic strategy for today?")
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        btnVoice.setOnClickListener {
            startSpeechRecognition()
        }

        btnAttachPhoto.setOnClickListener {
            showPhotoSourceDialog()
        }

        // Welcome Message
        if (messagesList.isEmpty()) {
            messagesList.add(
                ChatMessage(
                    id = "msg_welcome",
                    role = "assistant",
                    content = "Hey Dost! 👋 I am your MyBuddy AI tutor. I speak 8 languages and have live access to your timetable, assignments, and attendance.\n\nAsk me anything, speak via voice, or tap the camera icon to analyze a photo of your timetable or syllabus!",
                    sources = "Source: MyBuddy AI Core"
                )
            )
            adapter.notifyDataSetChanged()
        }

        return root
    }

    private fun setupLanguageChips() {
        val chips = listOf(
            Pair(chipLangEnglish, "English"),
            Pair(chipLangHindi, "Hindi"),
            Pair(chipLangHinglish, "Hinglish"),
            Pair(chipLangKannada, "Kannada"),
            Pair(chipLangTelugu, "Telugu"),
            Pair(chipLangTamil, "Tamil"),
            Pair(chipLangMarathi, "Marathi"),
            Pair(chipLangSpanish, "Spanish")
        )

        for ((chip, lang) in chips) {
            chip.setOnClickListener {
                currentLanguage = lang
                updateTtsLocale(lang)

                // Update UI state
                for ((otherChip, otherLang) in chips) {
                    if (otherLang == lang) {
                        otherChip.setBackgroundResource(R.drawable.bg_neumorphic_btn_primary_idle)
                        otherChip.setTextColor(resources.getColor(R.color.white, null))
                    } else {
                        otherChip.setBackgroundResource(R.drawable.bg_neumorphic_chip_idle)
                        otherChip.setTextColor(resources.getColor(R.color.text_primary, null))
                    }
                }
                Toast.makeText(context, "Language switched to $lang", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTtsLocale(lang: String) {
        val targetLocale = when (lang) {
            "Hindi", "Hinglish" -> Locale("hi", "IN")
            "Kannada" -> Locale("kn", "IN")
            "Telugu" -> Locale("te", "IN")
            "Tamil" -> Locale("ta", "IN")
            "Marathi" -> Locale("mr", "IN")
            "Spanish" -> Locale("es", "ES")
            else -> Locale("en", "IN")
        }
        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.ENGLISH
        }
        tts?.setPitch(1.12f)
        tts?.setSpeechRate(1.05f)
    }

    private fun speakTextFriendly(text: String) {
        try {
            val audioManager = context?.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            val maxVol = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15
            val currentVol = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0
            if (currentVol < (maxVol * 0.75).toInt()) {
                audioManager?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (maxVol * 0.85).toInt(), 0)
            }

            // Remove markdown characters for natural, fluent voice output
            val cleanText = text
                .replace(Regex("[•*#_`~>|\\[\\]()]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "MyBuddyTTS")
        } catch (_: Exception) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MyBuddyTTS")
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("📸 Take Photo with Camera", "🖼️ Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("AI Vision: Scan Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun processSelectedImage(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
            analyzeBitmapWithAI(bitmap)
        } catch (_: Exception) {
            Toast.makeText(context, "Failed to load image. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeBitmapWithAI(bitmap: Bitmap) {
        val userMsg = ChatMessage(
            role = "user",
            content = "📸 [Shared document/photo for AI analysis]"
        )
        messagesList.add(userMsg)
        adapter.notifyItemInserted(messagesList.size - 1)
        rvMessages.smoothScrollToPosition(messagesList.size - 1)

        val thinkingMsg = ChatMessage(
            role = "assistant",
            content = "🤖 Dost is analyzing your photo using AI Vision...",
            sources = "Processing on-device AI"
        )
        messagesList.add(thinkingMsg)
        val thinkingPos = messagesList.size - 1
        adapter.notifyItemInserted(thinkingPos)

        viewLifecycleOwner.lifecycleScope.launch {
            val timetableResults = AIVisionAnalyzer.analyzeTimetablePhoto(bitmap)
            val attendanceResults = AIVisionAnalyzer.analyzeAttendancePhoto(bitmap)

            messagesList.removeAt(thinkingPos)
            adapter.notifyItemRemoved(thinkingPos)

            val replyContent = when {
                timetableResults.isNotEmpty() -> {
                    val preview = timetableResults.take(6).joinToString("\n") {
                        "• ${it.dayOfWeek}: ${it.subjectName} (${it.startTime} - ${it.endTime}) [${it.classroom}]"
                    }
                    "Dost AI Vision detected ${timetableResults.size} class(es) in your timetable photo:\n\n$preview\n\nWould you like me to import these into your weekly schedule?"
                }
                attendanceResults.isNotEmpty() -> {
                    val preview = attendanceResults.take(5).joinToString("\n") {
                        "• ${it.subjectName}: ${it.attended}/${it.total} (${it.percentage}%)"
                    }
                    "Dost AI Vision extracted attendance figures from your photo:\n\n$preview\n\nAll detected records are ready to sync!"
                }
                else -> {
                    "Dost analyzed the photo, but could not detect clear timetable or attendance columns. Please ensure good lighting and text clarity, or enter them directly!"
                }
            }

            val reply = ChatMessage(
                role = "assistant",
                content = replyContent,
                sources = "Source: MyBuddy AI Vision Engine"
            )
            messagesList.add(reply)
            adapter.notifyItemInserted(messagesList.size - 1)
            rvMessages.smoothScrollToPosition(messagesList.size - 1)

            // If timetable was found, offer one-tap import
            if (timetableResults.isNotEmpty()) {
                offerTimetableImport(timetableResults)
            }
        }
    }

    private fun offerTimetableImport(entries: List<TimetableEntry>) {
        AlertDialog.Builder(requireContext())
            .setTitle("✨ Import Detected Timetable?")
            .setMessage("Found ${entries.size} classes in the image. Would you like to automatically add them to your weekly schedule?")
            .setPositiveButton("Import All") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    for (entry in entries) {
                        FirebaseRepository.addTimetableEntry(entry)
                    }
                    Toast.makeText(context, "Successfully imported ${entries.size} classes into your Timetable!", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startSpeechRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask MyBuddy Dost...")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voice recognition is not supported on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage(queryText: String) {
        val userMsg = ChatMessage(role = "user", content = queryText)
        messagesList.add(userMsg)
        adapter.notifyItemInserted(messagesList.size - 1)
        rvMessages.smoothScrollToPosition(messagesList.size - 1)
        etMessage.setText("")

        viewLifecycleOwner.lifecycleScope.launch {
            val reply = FirebaseRepository.answerQuestion(queryText, currentLanguage)
            messagesList.add(reply)
            adapter.notifyItemInserted(messagesList.size - 1)
            rvMessages.smoothScrollToPosition(messagesList.size - 1)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setPitch(1.12f)
            tts?.setSpeechRate(1.05f)
            updateTtsLocale(currentLanguage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts?.stop()
        tts?.shutdown()
    }
}
