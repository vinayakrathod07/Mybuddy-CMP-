package com.academialife.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StudentAIEngine {

    private var cachedInsight: AIProactiveInsight? = null

    suspend fun runProactiveAnalysis(): AIProactiveInsight = withContext(Dispatchers.IO) {
        try {
            val timetable = FirebaseRepository.getTimetable()
            val tasks = FirebaseRepository.getTasks()
            val subjects = FirebaseRepository.getSubjects()
            val records = FirebaseRepository.getAttendanceRecords()
            val profile = FirebaseRepository.getProfile()
            val wellness = FirebaseRepository.getTodayWellness()
            val gamification = FirebaseRepository.getGamification()

            val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records, profile.minimumAttendanceTarget)

            val todayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
            val todaysClasses = timetable.filter { it.dayOfWeek.equals(todayName, ignoreCase = true) }
            val pendingTasks = tasks.filter { it.status != "completed" }
            val urgentTasks = pendingTasks.filter { it.priorityScore >= 70.0 || it.isOverdue }

            val messageParts = mutableListOf<String>()
            var suggestedAction = "Explore Schedule"
            var actionTarget = "timetable"
            var warningSubject = ""

            // 1. Attendance Check
            val criticalSub = attSummary.subjectSummaries.firstOrNull { it.status == "critical" || it.status == "warning" }
            if (criticalSub != null) {
                warningSubject = criticalSub.subjectName
                messageParts.add("⚠️ Attendance Alert: '${criticalSub.subjectName}' is at ${criticalSub.percentage}%. Attend next ${criticalSub.classesNeeded} class(es) to stay compliant.")
                suggestedAction = "View Attendance"
                actionTarget = "attendance"
            }

            // 2. Urgent Deadlines
            if (urgentTasks.isNotEmpty()) {
                val topTask = urgentTasks.maxByOrNull { it.priorityScore }
                messageParts.add("⚡ High Priority: '${topTask?.title}' due ${topTask?.dueDate} (${topTask?.priorityScore?.toInt()} pts).")
                if (criticalSub == null) {
                    suggestedAction = "Open Planner"
                    actionTarget = "tasks"
                }
            }

            // 3. Today's Lectures
            if (todaysClasses.isNotEmpty()) {
                messageParts.add("🗓️ You have ${todaysClasses.size} lecture(s) today, starting with ${todaysClasses.first().subjectName} at ${todaysClasses.first().startTime}.")
            } else {
                messageParts.add("🎉 No classes scheduled today! Great opportunity for study blocks or resting.")
            }

            // 4. Hydration & Habits
            if (wellness.waterGlasses < 4) {
                messageParts.add("💧 Tip: You've had ${wellness.waterGlasses}/8 glasses of water today. Drink 1 glass to boost focus (+15 XP)!")
            }

            val finalTitle = if (urgentTasks.isNotEmpty() || criticalSub != null) {
                "⚡ Dost's Action Required Strategy"
            } else {
                "✨ Dost's Optimal Routine Strategy"
            }

            val combinedMessage = messageParts.joinToString(" ")

            val insight = AIProactiveInsight(
                title = finalTitle,
                message = combinedMessage,
                urgentCount = urgentTasks.size,
                attendanceWarningSubject = warningSubject,
                suggestedAction = suggestedAction,
                actionTarget = actionTarget,
                timestamp = System.currentTimeMillis()
            )

            cachedInsight = insight
            insight
        } catch (_: Exception) {
            AIProactiveInsight()
        }
    }

    fun getLatestInsight(): AIProactiveInsight {
        return cachedInsight ?: AIProactiveInsight()
    }
}

