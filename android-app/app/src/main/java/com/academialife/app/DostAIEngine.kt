package com.academialife.app

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DostAIEngine {

    private const val PREFS_AI = "mybuddy_ai_config"
    private const val KEY_CUSTOM_GEMINI_KEY = "custom_gemini_api_key"

    fun getApiKey(context: Context): String {
        val customKey = context.getSharedPreferences(PREFS_AI, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_GEMINI_KEY, "") ?: ""
        if (customKey.isNotBlank()) return customKey
        return BuildConfig.GEMINI_API_KEY
    }

    fun setCustomApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_AI, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_GEMINI_KEY, key.trim())
            .apply()
    }

    suspend fun queryDost(
        context: Context,
        userQuery: String,
        language: String = "English",
        recentHistory: List<ChatMessage> = emptyList()
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)

        // 1. If Gemini key is available, execute real cloud model with grounded student context
        if (apiKey.isNotBlank()) {
            try {
                val groundedContext = buildStudentAcademicContext(language)
                val conversationSummary = recentHistory.takeLast(4).joinToString("\n") {
                    "${if (it.role == "user") "Student" else "Dost"}: ${it.content.take(120)}"
                }

                val systemPrompt = """
                    You are 'Dost' (which means Friend), a warm, supportive, motivating, and intelligent personal AI academic tutor in the MyBuddy Android app.
                    Your student user relies on you for classes, timetable, attendance, study schedules, notes, and academic guidance.
                    Answer the student in their selected language: $language.
                    
                    STUDENT'S LIVE ACADEMIC RECORDS (AUTHENTICATED & GROUNDED):
                    $groundedContext
                    
                    RECENT CONVERSATION:
                    $conversationSummary
                    
                    INSTRUCTIONS:
                    1. Ground your answers strictly in the student's records above.
                    2. If asked about today's classes, exams, or attendance, use the exact data provided.
                    3. If the student asks for something not in the records, state it honestly and suggest adding it.
                    4. Keep your tone encouraging, youthful, and concise (bullet points where appropriate).
                    5. Never ask for or expose Aadhaar, PAN, passwords, or payment cards.
                """.trimIndent()

                val model = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey
                )

                val fullPrompt = "$systemPrompt\n\nStudent asks: $userQuery"
                val response = model.generateContent(fullPrompt)
                val replyText = response.text?.trim()

                if (!replyText.isNullOrEmpty()) {
                    return@withContext ChatMessage(
                        role = "assistant",
                        content = replyText,
                        sources = "Source: Gemini 1.5 Flash • MyBuddy AI Cloud"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("DostAIEngine", "Gemini Cloud failed or rate-limited: ${e.message}")
            }
        }

        // 2. Fallback to Local Grounded Academic Engine with honest attribution
        val localReply = FirebaseRepository.answerQuestion(userQuery, language)
        val sourceAttr = if (apiKey.isBlank()) {
            "Source: MyBuddy Local Academic Engine • Add Gemini API Key in Profile for Cloud LLM"
        } else {
            "Source: MyBuddy Local Academic Engine (Offline Fallback)"
        }

        return@withContext localReply.copy(sources = sourceAttr)
    }

    private suspend fun buildStudentAcademicContext(language: String): String {
        return try {
            val profile = FirebaseRepository.getProfile()
            val timetable = FirebaseRepository.getTimetable()
            val tasks = FirebaseRepository.getTasks()
            val subjects = FirebaseRepository.getSubjects()
            val records = FirebaseRepository.getAttendanceRecords()
            val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records, profile.minimumAttendanceTarget)
            val todayDay = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
            val todaysClasses = timetable.filter { it.dayOfWeek.equals(todayDay, ignoreCase = true) }
            val pendingTasks = tasks.filter { it.status != "completed" }

            val classesInfo = if (todaysClasses.isNotEmpty()) {
                todaysClasses.joinToString("; ") { "${it.subjectName} (${it.startTime}-${it.endTime}) [${it.classroom}]" }
            } else {
                "No classes scheduled for today ($todayDay) - Rest/Study Day"
            }

            val tasksInfo = if (pendingTasks.isNotEmpty()) {
                pendingTasks.take(5).joinToString("; ") { "${it.title} (due ${it.dueDate}, priority: ${it.priorityScore})" }
            } else {
                "All coursework tasks are completed"
            }

            val attendanceInfo = "Overall: ${attSummary.overallPercentage}% (Target: ${profile.minimumAttendanceTarget}%). ${
                attSummary.subjectSummaries.joinToString("; ") { "${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses})" }
            }"

            """
            • Student Name: ${profile.fullName}
            • College/Course: ${profile.college} - ${profile.course} (${profile.semester})
            • Today is: $todayDay
            • Today's Timetable: $classesInfo
            • Pending Assignments/Deadlines: $tasksInfo
            • Live Attendance Compliance: $attendanceInfo
            • Active Study Streak: ${FirebaseRepository.getGamification().streakDays} days
            """.trimIndent()
        } catch (_: Exception) {
            "Student records currently loading."
        }
    }
}

