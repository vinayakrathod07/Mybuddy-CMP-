package com.academialife.app

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseRepository {

    // Database ID assigned for this applet
    private const val FIRESTORE_DATABASE_ID = "ai-studio-academialifestud-faab33c7-9821-445b-9c14-7c8200013f6a"

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FIRESTORE_DATABASE_ID)
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "guest_student_demo"
    }

    suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    // 1. Profile
    suspend fun getProfile(): StudentProfile {
        val uid = getCurrentUserId()
        val doc = firestore.collection("users").document(uid).get().await()
        return if (doc.exists()) {
            StudentProfile(
                userId = uid,
                email = doc.getString("email") ?: auth.currentUser?.email ?: "student@academic.edu",
                fullName = doc.getString("fullName") ?: "Student",
                college = doc.getString("college") ?: "National Institute of Technology",
                course = doc.getString("course") ?: "B.Tech Computer Science & Engineering",
                semester = doc.getString("semester") ?: "Semester 6",
                academicYear = doc.getString("academicYear") ?: "2026-2027",
                minimumAttendanceTarget = doc.getDouble("minimumAttendanceTarget") ?: 75.0
            )
        } else {
            val p = StudentProfile(
                userId = uid,
                email = auth.currentUser?.email ?: "student@academic.edu",
                fullName = "Student"
            )
            firestore.collection("users").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "email" to p.email,
                    "fullName" to p.fullName,
                    "college" to p.college,
                    "course" to p.course,
                    "semester" to p.semester,
                    "academicYear" to p.academicYear,
                    "minimumAttendanceTarget" to p.minimumAttendanceTarget
                )
            ).await()
            p
        }
    }

    // 2. Subjects
    suspend fun getSubjects(): List<Subject> {
        val uid = getCurrentUserId()
        val snap = firestore.collection("users").document(uid).collection("subjects").get().await()
        return snap.documents.map { d ->
            Subject(
                id = d.id,
                userId = uid,
                name = d.getString("name") ?: "",
                code = d.getString("code") ?: "",
                faculty = d.getString("faculty") ?: "",
                color = d.getString("color") ?: "#4F46E5",
                targetAttendance = d.getDouble("targetAttendance") ?: 75.0
            )
        }.sortedBy { it.name }
    }

    suspend fun addSubject(name: String, code: String, faculty: String, color: String = "#4F46E5"): Subject {
        val uid = getCurrentUserId()
        val id = "sub_" + UUID.randomUUID().toString().take(8)
        val sub = Subject(id = id, userId = uid, name = name, code = code.uppercase(), faculty = faculty, color = color)
        firestore.collection("users").document(uid).collection("subjects").document(id).set(
            mapOf(
                "userId" to uid,
                "name" to sub.name,
                "code" to sub.code,
                "faculty" to sub.faculty,
                "color" to sub.color,
                "targetAttendance" to sub.targetAttendance,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        return sub
    }

    // 3. Timetable
    suspend fun getTimetable(): List<TimetableEntry> {
        val uid = getCurrentUserId()
        val snap = firestore.collection("users").document(uid).collection("timetable").get().await()
        return snap.documents.map { d ->
            TimetableEntry(
                id = d.id,
                userId = uid,
                subjectId = d.getString("subjectId") ?: "",
                subjectName = d.getString("subjectName") ?: "",
                subjectCode = d.getString("subjectCode") ?: "",
                subjectColor = d.getString("subjectColor") ?: "#4F46E5",
                dayOfWeek = d.getString("dayOfWeek") ?: "Monday",
                startTime = d.getString("startTime") ?: "09:00",
                endTime = d.getString("endTime") ?: "10:30",
                classroom = d.getString("classroom") ?: "",
                facultyName = d.getString("facultyName") ?: "",
                notes = d.getString("notes") ?: ""
            )
        }.sortedBy { it.startTime }
    }

    suspend fun addTimetableEntry(entry: TimetableEntry): TimetableEntry {
        val uid = getCurrentUserId()
        val id = "tt_" + UUID.randomUUID().toString().take(8)
        val item = entry.copy(id = id, userId = uid)
        firestore.collection("users").document(uid).collection("timetable").document(id).set(
            mapOf(
                "userId" to uid,
                "subjectId" to item.subjectId,
                "subjectName" to item.subjectName,
                "subjectCode" to item.subjectCode,
                "subjectColor" to item.subjectColor,
                "dayOfWeek" to item.dayOfWeek,
                "startTime" to item.startTime,
                "endTime" to item.endTime,
                "classroom" to item.classroom,
                "facultyName" to item.facultyName,
                "notes" to item.notes
            )
        ).await()
        return item
    }

    // 4. Tasks
    suspend fun getTasks(): List<Task> {
        val uid = getCurrentUserId()
        val snap = firestore.collection("users").document(uid).collection("tasks").get().await()
        return snap.documents.map { d ->
            val title = d.getString("title") ?: ""
            val taskType = d.getString("taskType") ?: "assignment"
            val priority = d.getString("priority") ?: "medium"
            val dueDate = d.getString("dueDate") ?: ""
            val effort = d.getLong("estimatedEffortMinutes")?.toInt() ?: 60
            val status = d.getString("status") ?: "pending"

            val (score, reason) = AcademicCalculators.calculatePriorityScore(
                taskType, priority, dueDate, effort, status == "completed"
            )

            Task(
                id = d.id,
                userId = uid,
                subjectId = d.getString("subjectId") ?: "",
                subjectName = d.getString("subjectName") ?: "",
                subjectCode = d.getString("subjectCode") ?: "",
                subjectColor = d.getString("subjectColor") ?: "#4F46E5",
                title = title,
                description = d.getString("description") ?: "",
                taskType = taskType,
                dueDate = dueDate,
                estimatedEffortMinutes = effort,
                priority = priority,
                priorityScore = score,
                priorityReason = reason,
                status = status
            )
        }.sortedByDescending { it.priorityScore }
    }

    suspend fun addTask(task: Task): Task {
        val uid = getCurrentUserId()
        val id = "tsk_" + UUID.randomUUID().toString().take(8)
        val (score, reason) = AcademicCalculators.calculatePriorityScore(
            task.taskType, task.priority, task.dueDate, task.estimatedEffortMinutes, false
        )
        val item = task.copy(id = id, userId = uid, priorityScore = score, priorityReason = reason)
        firestore.collection("users").document(uid).collection("tasks").document(id).set(
            mapOf(
                "userId" to uid,
                "subjectId" to item.subjectId,
                "subjectName" to item.subjectName,
                "subjectCode" to item.subjectCode,
                "title" to item.title,
                "description" to item.description,
                "taskType" to item.taskType,
                "dueDate" to item.dueDate,
                "estimatedEffortMinutes" to item.estimatedEffortMinutes,
                "priority" to item.priority,
                "priorityScore" to item.priorityScore,
                "priorityReason" to item.priorityReason,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        return item
    }

    suspend fun updateTaskStatus(taskId: String, status: String) {
        val uid = getCurrentUserId()
        firestore.collection("users").document(uid).collection("tasks").document(taskId)
            .update("status", status).await()
    }

    // 5. Attendance
    suspend fun getAttendanceRecords(): List<AttendanceRecord> {
        val uid = getCurrentUserId()
        val snap = firestore.collection("users").document(uid).collection("attendance").get().await()
        return snap.documents.map { d ->
            AttendanceRecord(
                id = d.id,
                userId = uid,
                subjectId = d.getString("subjectId") ?: "",
                subjectName = d.getString("subjectName") ?: "",
                subjectCode = d.getString("subjectCode") ?: "",
                date = d.getString("date") ?: "",
                status = d.getString("status") ?: "present",
                notes = d.getString("notes") ?: ""
            )
        }.sortedByDescending { it.date }
    }

    suspend fun recordAttendance(subjectId: String, subjectName: String, subjectCode: String, status: String): AttendanceRecord {
        val uid = getCurrentUserId()
        val id = "att_" + UUID.randomUUID().toString().take(8)
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val item = AttendanceRecord(
            id = id,
            userId = uid,
            subjectId = subjectId,
            subjectName = subjectName,
            subjectCode = subjectCode,
            date = todayStr,
            status = status
        )
        firestore.collection("users").document(uid).collection("attendance").document(id).set(
            mapOf(
                "userId" to uid,
                "subjectId" to subjectId,
                "subjectName" to subjectName,
                "subjectCode" to subjectCode,
                "date" to todayStr,
                "status" to status,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        return item
    }

    // 6. Assessments
    suspend fun getAssessments(): List<Assessment> {
        val uid = getCurrentUserId()
        val snap = firestore.collection("users").document(uid).collection("assessments").get().await()
        return snap.documents.map { d ->
            val obtained = d.getDouble("marksObtained") ?: 0.0
            val maxMarks = d.getDouble("maximumMarks") ?: 20.0
            val pct = if (maxMarks > 0) kotlin.math.round((obtained / maxMarks) * 1000.0) / 10.0 else 0.0
            Assessment(
                id = d.id,
                userId = uid,
                subjectId = d.getString("subjectId") ?: "",
                subjectName = d.getString("subjectName") ?: "",
                subjectCode = d.getString("subjectCode") ?: "",
                assessmentName = d.getString("assessmentName") ?: "",
                assessmentType = d.getString("assessmentType") ?: "Quiz",
                marksObtained = obtained,
                maximumMarks = maxMarks,
                percentage = pct,
                assessmentDate = d.getString("assessmentDate") ?: ""
            )
        }.sortedByDescending { it.assessmentDate }
    }

    suspend fun addAssessment(assessment: Assessment): Assessment {
        val uid = getCurrentUserId()
        val id = "ass_" + UUID.randomUUID().toString().take(8)
        val maxMarks = if (assessment.maximumMarks > 0) assessment.maximumMarks else 20.0
        val pct = kotlin.math.round((assessment.marksObtained / maxMarks) * 1000.0) / 10.0
        val item = assessment.copy(id = id, userId = uid, percentage = pct)
        firestore.collection("users").document(uid).collection("assessments").document(id).set(
            mapOf(
                "userId" to uid,
                "subjectId" to item.subjectId,
                "subjectName" to item.subjectName,
                "subjectCode" to item.subjectCode,
                "assessmentName" to item.assessmentName,
                "assessmentType" to item.assessmentType,
                "marksObtained" to item.marksObtained,
                "maximumMarks" to item.maximumMarks,
                "percentage" to item.percentage,
                "assessmentDate" to item.assessmentDate,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        return item
    }

    // 7. AI Assistant RAG Response
    suspend fun answerQuestion(userQuery: String): ChatMessage {
        val uid = getCurrentUserId()
        val queryLower = userQuery.lowercase(java.util.Locale.ROOT)
        val timetable = getTimetable()
        val tasks = getTasks()
        val subjects = getSubjects()
        val records = getAttendanceRecords()
        val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records)

        var answer = ""
        var sourceText = ""

        if (queryLower.contains("class") || queryLower.contains("timetable") || queryLower.contains("schedule") || queryLower.contains("today")) {
            val todayDay = java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(java.util.Date())
            val todays = timetable.filter { it.dayOfWeek.equals(todayDay, ignoreCase = true) }
            if (todays.isNotEmpty()) {
                answer = "You have ${todays.size} class(es) today ($todayDay):\n" +
                        todays.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) in ${it.classroom}" }
                sourceText = "Source: Live Timetable schedule ($todayDay)"
            } else {
                answer = "You have no classes scheduled for today ($todayDay) in your timetable."
            }
        } else if (queryLower.contains("attendance") || queryLower.contains("percentage") || queryLower.contains("present")) {
            answer = "Overall attendance is ${attSummary.overallPercentage}% across ${attSummary.totalConducted} classes.\n" +
                    attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses}). ${it.explanation}" }
            sourceText = "Source: Firestore Attendance ledger"
        } else if (queryLower.contains("task") || queryLower.contains("assignment") || queryLower.contains("exam") || queryLower.contains("due")) {
            val pending = tasks.filter { it.status != "completed" }
            if (pending.isNotEmpty()) {
                answer = "You have ${pending.size} active tasks/exams:\n" +
                        pending.take(4).joinToString("\n") { "• ${it.title} [Priority: ${it.priorityScore} pts] - ${it.priorityReason}" }
                sourceText = "Source: Tasks & Examination Registry"
            } else {
                answer = "You have zero pending tasks or exams right now! All coursework is completed."
            }
        } else {
            answer = "Hello! I am your AcademiaLife AI assistant. I have live access to your timetable, coursework deadlines, attendance compliance, and exam marks stored in Firebase."
        }

        return ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            role = "assistant",
            content = answer,
            sources = sourceText
        )
    }
}
