package com.academialife.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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

    private lateinit var adapter: ChatAdapter
    private var tts: TextToSpeech? = null
    private val messagesList = mutableListOf<ChatMessage>()

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

        tts = TextToSpeech(requireContext(), this)

        adapter = ChatAdapter(messagesList) { textToSpeak ->
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "AcademiaLifeTTS")
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

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        btnVoice.setOnClickListener {
            startSpeechRecognition()
        }

        // Welcome Message
        if (messagesList.isEmpty()) {
            messagesList.add(
                ChatMessage(
                    id = "msg_welcome",
                    role = "assistant",
                    content = "Hello! I am your AcademiaLife AI tutor. I have live access to your timetable, assignment priorities, and attendance compliance.\n\nAsk me anything or tap the microphone to speak!",
                    sources = "Source: Live Academic Assistant Core"
                )
            )
            adapter.notifyDataSetChanged()
        }

        return root
    }

    private fun startSpeechRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask AcademiaLife AI Tutor...")
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
            val reply = FirebaseRepository.answerQuestion(queryText)
            messagesList.add(reply)
            adapter.notifyItemInserted(messagesList.size - 1)
            rvMessages.smoothScrollToPosition(messagesList.size - 1)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts?.stop()
        tts?.shutdown()
    }
}
