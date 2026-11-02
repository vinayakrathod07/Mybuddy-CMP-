package com.academialife.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DostAIEngine {

    suspend fun queryDost(
        context: Context,
        userQuery: String,
        language: String = "English",
        recentHistory: List<ChatMessage> = emptyList()
    ): ChatMessage = withContext(Dispatchers.IO) {
        val reply = FirebaseRepository.answerQuestion(userQuery, language)
        reply.copy(sources = "Source: Dost AI Assistant")
    }
}
