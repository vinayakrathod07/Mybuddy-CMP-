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
import android.widget.ImageButton
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
    private lateinit var btnVoice: ImageButton
    private lateinit var btnSend: ImageButton

    private lateinit var adapter: ChatAdapter
    private var tts: TextToSpeech? = null

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
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

        adapter = ChatAdapter(emptyList()) { textToSpeak ->
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "AcademiaLifeTTS")
        }

        rvMessages.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        btnVoice.setOnClickListener {
            startVoiceRecognition()
        }

        // Add welcome message
        adapter.addMessage(
            ChatMessage(
                role = "assistant",
                content = "Hello! I am your AcademiaLife AI tutor. You can ask me:\n• What classes do I have today?\n• Check my attendance compliance standing\n• Which assignments or exams are due soon?"
            )
        )

        return root
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask your Academic Assistant...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voice recognition not supported on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage(query: String) {
        etMessage.text.clear()
        adapter.addMessage(ChatMessage(role = "user", content = query))
        rvMessages.scrollToPosition(adapter.itemCount - 1)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val assistantMsg = FirebaseRepository.answerQuestion(query)
                adapter.addMessage(assistantMsg)
                rvMessages.scrollToPosition(adapter.itemCount - 1)
            } catch (e: Exception) {
                adapter.addMessage(
                    ChatMessage(
                        role = "assistant",
                        content = "Sorry, I encountered an issue analyzing your coursework records: ${e.message}"
                    )
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
