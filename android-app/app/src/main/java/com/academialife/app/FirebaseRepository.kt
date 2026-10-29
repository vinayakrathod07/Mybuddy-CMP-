package com.academialife.app

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FirebaseRepository {

    private const val FIRESTORE_DATABASE_ID = "ai-studio-academialifestud-faab33c7-9821-445b-9c14-7c8200013f6a"

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            try {
                FirebaseFirestore.getInstance(FirebaseApp.getInstance(), FIRESTORE_DATABASE_ID)
            } catch (_: Exception) {
                FirebaseFirestore.getInstance()
            }
        }
    }

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "guest_student_demo"
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null && !auth.currentUser!!.isAnonymous
    }

    suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
            } catch (_: Exception) {}
        }
    }

    suspend fun signInEmail(email: String, pass: String): Pair<Boolean, String?> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: e.message ?: "Sign in failed")
        }
    }

    suspend fun registerEmail(email: String, pass: String, fullName: String): Pair<Boolean, String?> {
        return try {
            val res = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = res.user?.uid ?: return Pair(false, "Account creation failed")
            val profile = StudentProfile(
                userId = uid,
                email = email,
                fullName = fullName
            )
            saveProfile(profile)
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: e.message ?: "Registration failed")
        }
    }

    suspend fun sendPasswordReset(email: String): Pair<Boolean, String?> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: e.message ?: "Failed to send reset email")
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // ==========================================
    // 1. STUDENT PROFILE & SETTINGS
    // ==========================================
    suspend fun getProfile(): StudentProfile {
        val uid = getCurrentUserId()
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                StudentProfile(
                    userId = uid,
                    email = doc.getString("email") ?: auth.currentUser?.email ?: "student@academic.edu",
                    fullName = doc.getString("fullName") ?: "Student",
                    college = doc.getString("college") ?: "National Institute of Technology",
                    course = doc.getString("course") ?: "B.Tech Computer Science & Engineering",
                    semester = doc.getString("semester") ?: "Semester 6",
                    academicYear = doc.getString("academicYear") ?: "2026-2027",
                    minimumAttendanceTarget = doc.getDouble("minimumAttendanceTarget") ?: 75.0,
                    preferredStudyHoursPerDay = doc.getDouble("preferredStudyHoursPerDay") ?: 4.0,
                    preferredBreakDurationMinutes = doc.getLong("preferredBreakDurationMinutes")?.toInt() ?: 15
                )
            } else {
                val p = StudentProfile(
                    userId = uid,
                    email = auth.currentUser?.email ?: "student@academic.edu",
                    fullName = "Student"
                )
                saveProfile(p)
                p
            }
        } catch (_: Exception) {
            StudentProfile(userId = uid, email = "student@academic.edu", fullName = "Student")
        }
    }

    suspend fun saveProfile(profile: StudentProfile): Boolean {
        val uid = getCurrentUserId()
        return try {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "email" to profile.email,
                    "fullName" to profile.fullName,
                    "college" to profile.college,
                    "course" to profile.course,
                    "semester" to profile.semester,
                    "academicYear" to profile.academicYear,
                    "minimumAttendanceTarget" to profile.minimumAttendanceTarget,
                    "preferredStudyHoursPerDay" to profile.preferredStudyHoursPerDay,
                    "preferredBreakDurationMinutes" to profile.preferredBreakDurationMinutes
                )
            ).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "saveProfile failed: ${e.message}")
            false
        }
    }

    // ==========================================
    // 2. SUBJECTS
    // ==========================================
    suspend fun getSubjects(): List<Subject> {
        val uid = getCurrentUserId()
        return try {
            val snap = firestore.collection("users").document(uid).collection("subjects").get().await()
            snap.documents.map { d ->
                Subject(
                    id = d.id,
                    userId = uid,
                    name = d.getString("name") ?: "",
                    code = d.getString("code") ?: "",
                    faculty = d.getString("faculty") ?: "",
                    color = d.getString("color") ?: "#8175E5",
                    targetAttendance = d.getDouble("targetAttendance") ?: 75.0
                )
            }.sortedBy { it.name }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addSubject(name: String, code: String, faculty: String, color: String = "#8175E5", targetAttendance: Double = 75.0): Subject {
        val uid = getCurrentUserId()
        val id = "sub_" + UUID.randomUUID().toString().take(8)
        val sub = Subject(
            id = id,
            userId = uid,
            name = name,
            code = code.uppercase(),
            faculty = faculty,
            color = color,
            targetAttendance = targetAttendance
        )
        try {
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
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "addSubject failed: ${e.message}")
        }
        return sub
    }

    suspend fun deleteSubject(subjectId: String): Boolean {
        val uid = getCurrentUserId()
        return try {
            firestore.collection("users").document(uid).collection("subjects").document(subjectId).delete().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "deleteSubject failed: ${e.message}")
            false
        }
    }

    // ==========================================
    // 3. TIMETABLE
    // ==========================================
    suspend fun getTimetable(): List<TimetableEntry> {
        val uid = getCurrentUserId()
        return try {
            val snap = firestore.collection("users").document(uid).collection("timetable").get().await()
            snap.documents.map { d ->
                TimetableEntry(
                    id = d.id,
                    userId = uid,
                    subjectId = d.getString("subjectId") ?: "",
                    subjectName = d.getString("subjectName") ?: "",
                    subjectCode = d.getString("subjectCode") ?: "",
                    subjectColor = d.getString("subjectColor") ?: "#8175E5",
                    dayOfWeek = d.getString("dayOfWeek") ?: "Monday",
                    startTime = d.getString("startTime") ?: "09:00",
                    endTime = d.getString("endTime") ?: "10:30",
                    classroom = d.getString("classroom") ?: "",
                    facultyName = d.getString("facultyName") ?: "",
                    notes = d.getString("notes") ?: ""
                )
            }.sortedBy { it.startTime }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addTimetableEntry(entry: TimetableEntry): TimetableEntry {
        val uid = getCurrentUserId()
        val id = "tt_" + UUID.randomUUID().toString().take(8)
        val item = entry.copy(id = id, userId = uid)
        try {
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
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "addTimetableEntry failed: ${e.message}")
        }
        return item
    }

    suspend fun deleteTimetableEntry(id: String): Boolean {
        val uid = getCurrentUserId()
        return try {
            firestore.collection("users").document(uid).collection("timetable").document(id).delete().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "deleteTimetableEntry failed: ${e.message}")
            false
        }
    }

    // ==========================================
    // 4. TASKS & EXAMS
    // ==========================================
    suspend fun getTasks(): List<Task> {
        val uid = getCurrentUserId()
        return try {
            val snap = firestore.collection("users").document(uid).collection("tasks").get().await()
            snap.documents.map { d ->
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
                    subjectColor = d.getString("subjectColor") ?: "#8175E5",
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
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addTask(task: Task): Task {
        val uid = getCurrentUserId()
        val id = "tsk_" + UUID.randomUUID().toString().take(8)
        val (score, reason) = AcademicCalculators.calculatePriorityScore(
            task.taskType, task.priority, task.dueDate, task.estimatedEffortMinutes, false
        )
        val item = task.copy(id = id, userId = uid, priorityScore = score, priorityReason = reason)
        try {
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
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "addTask failed: ${e.message}")
        }
        return item
    }

    suspend fun updateTaskStatus(taskId: String, status: String): Boolean {
        val uid = getCurrentUserId()
        return try {
            firestore.collection("users").document(uid).collection("tasks").document(taskId)
                .update("status", status).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "updateTaskStatus failed: ${e.message}")
            false
        }
    }

    suspend fun deleteTask(taskId: String): Boolean {
        val uid = getCurrentUserId()
        return try {
            firestore.collection("users").document(uid).collection("tasks").document(taskId).delete().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "deleteTask failed: ${e.message}")
            false
        }
    }

    // ==========================================
    // 5. ATTENDANCE
    // ==========================================
    suspend fun getAttendanceRecords(): List<AttendanceRecord> {
        val uid = getCurrentUserId()
        return try {
            val snap = firestore.collection("users").document(uid).collection("attendance").get().await()
            snap.documents.map { d ->
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
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun recordAttendance(subjectId: String, subjectName: String, subjectCode: String, status: String): AttendanceRecord {
        val uid = getCurrentUserId()
        val id = "att_" + UUID.randomUUID().toString().take(8)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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

    suspend fun deleteAttendanceRecord(id: String) {
        val uid = getCurrentUserId()
        firestore.collection("users").document(uid).collection("attendance").document(id).delete().await()
    }

    // ==========================================
    // 6. ASSESSMENTS / MARKS
    // ==========================================
    suspend fun getAssessments(): List<Assessment> {
        val uid = getCurrentUserId()
        return try {
            val snap = firestore.collection("users").document(uid).collection("assessments").get().await()
            snap.documents.map { d ->
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
        } catch (_: Exception) {
            emptyList()
        }
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

    suspend fun deleteAssessment(id: String) {
        val uid = getCurrentUserId()
        firestore.collection("users").document(uid).collection("assessments").document(id).delete().await()
    }

    // ==========================================
    // 7. STUDY PLANNER & FOCUS SESSIONS
    // ==========================================
    suspend fun getStudySessions(): List<StudySession> {
        val uid = getCurrentUserId()
        return try {
            val snap = firestore.collection("users").document(uid).collection("study_sessions").get().await()
            snap.documents.map { d ->
                StudySession(
                    id = d.id,
                    userId = uid,
                    subjectId = d.getString("subjectId") ?: "",
                    subjectName = d.getString("subjectName") ?: "",
                    subjectCode = d.getString("subjectCode") ?: "",
                    taskId = d.getString("taskId") ?: "",
                    taskTitle = d.getString("taskTitle") ?: "",
                    title = d.getString("title") ?: "",
                    sessionDate = d.getString("sessionDate") ?: "",
                    startTime = d.getString("startTime") ?: "18:00",
                    endTime = d.getString("endTime") ?: "19:30",
                    durationMinutes = d.getLong("durationMinutes")?.toInt() ?: 90,
                    status = d.getString("status") ?: "scheduled",
                    recommendationReason = d.getString("recommendationReason") ?: "Self-scheduled block"
                )
            }.sortedBy { it.sessionDate + it.startTime }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addStudySession(session: StudySession): StudySession {
        val uid = getCurrentUserId()
        val id = "st_" + UUID.randomUUID().toString().take(8)
        val item = session.copy(id = id, userId = uid)
        firestore.collection("users").document(uid).collection("study_sessions").document(id).set(
            mapOf(
                "userId" to uid,
                "subjectId" to item.subjectId,
                "subjectName" to item.subjectName,
                "subjectCode" to item.subjectCode,
                "taskId" to item.taskId,
                "taskTitle" to item.taskTitle,
                "title" to item.title,
                "sessionDate" to item.sessionDate,
                "startTime" to item.startTime,
                "endTime" to item.endTime,
                "durationMinutes" to item.durationMinutes,
                "status" to item.status,
                "recommendationReason" to item.recommendationReason,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        return item
    }

    suspend fun updateStudySessionStatus(sessionId: String, status: String) {
        val uid = getCurrentUserId()
        firestore.collection("users").document(uid).collection("study_sessions").document(sessionId)
            .update("status", status).await()
    }

    suspend fun deleteStudySession(id: String) {
        val uid = getCurrentUserId()
        firestore.collection("users").document(uid).collection("study_sessions").document(id).delete().await()
    }

    /**
     * Auto-Generate Smart Study Plan based on upcoming tasks, exams, and daily hour targets
     */
    suspend fun autoGenerateStudyPlan(daysAhead: Int = 7, dailyHours: Double = 4.0): List<StudySession> {
        val tasks = getTasks().filter { it.status != "completed" }
        val subjects = getSubjects()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val generated = mutableListOf<StudySession>()

        val topTasks = tasks.sortedByDescending { it.priorityScore }.take(daysAhead * 2)
        var taskIdx = 0

        for (i in 0 until daysAhead) {
            val date = Date(System.currentTimeMillis() + (i * 24L * 3600L * 1000L))
            val dateStr = sdf.format(date)

            val currentTask = if (taskIdx < topTasks.size) topTasks[taskIdx++] else null
            val sub = if (currentTask != null) {
                subjects.firstOrNull { it.id == currentTask.subjectId }
            } else if (subjects.isNotEmpty()) {
                subjects[i % subjects.size]
            } else null

            val title = if (currentTask != null) "Focus: ${currentTask.title}" else "Revision: ${sub?.name ?: "Coursework"}"
            val reason = if (currentTask != null) "High priority task [${currentTask.priorityScore.toInt()} pts]" else "Balanced subject revision"

            val session = StudySession(
                subjectId = sub?.id ?: "",
                subjectName = sub?.name ?: "General Study",
                subjectCode = sub?.code ?: "",
                taskId = currentTask?.id ?: "",
                taskTitle = currentTask?.title ?: "",
                title = title,
                sessionDate = dateStr,
                startTime = "18:00",
                endTime = "19:30",
                durationMinutes = 90,
                status = "scheduled",
                recommendationReason = reason
            )
            addStudySession(session)
            generated.add(session)
        }
        return generated
    }

    // ==========================================
    // 8. DOCUMENT VAULT
    // ==========================================
    suspend fun getDocuments(): List<VaultDocument> {
        val uid = getCurrentUserId()
        return try {
            val snap = firestore.collection("users").document(uid).collection("documents").get().await()
            snap.documents.map { d ->
                VaultDocument(
                    id = d.id,
                    userId = uid,
                    subjectId = d.getString("subjectId") ?: "",
                    subjectName = d.getString("subjectName") ?: "",
                    title = d.getString("title") ?: "",
                    category = d.getString("category") ?: "Lecture Notes",
                    fileUrl = d.getString("fileUrl") ?: "",
                    fileSizeKb = d.getLong("fileSizeKb")?.toInt() ?: 150,
                    notes = d.getString("notes") ?: "",
                    createdAt = d.getString("createdAt") ?: ""
                )
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addDocument(doc: VaultDocument): VaultDocument {
        val uid = getCurrentUserId()
        val id = "doc_" + UUID.randomUUID().toString().take(8)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val item = doc.copy(id = id, userId = uid, createdAt = todayStr)
        firestore.collection("users").document(uid).collection("documents").document(id).set(
            mapOf(
                "userId" to uid,
                "subjectId" to item.subjectId,
                "subjectName" to item.subjectName,
                "title" to item.title,
                "category" to item.category,
                "fileUrl" to item.fileUrl,
                "fileSizeKb" to item.fileSizeKb,
                "notes" to item.notes,
                "createdAt" to todayStr
            )
        ).await()
        return item
    }

    suspend fun deleteDocument(id: String) {
        val uid = getCurrentUserId()
        firestore.collection("users").document(uid).collection("documents").document(id).delete().await()
    }

    // ==========================================
    // 9. SMART NOTIFICATIONS
    // ==========================================
    suspend fun getNotifications(): List<NotificationItem> {
        val uid = getCurrentUserId()
        val list = mutableListOf<NotificationItem>()

        // 1. Check Attendance warnings
        val subjects = getSubjects()
        val records = getAttendanceRecords()
        val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records)
        attSummary.subjectSummaries.forEach { sub ->
            if (sub.status == "critical") {
                list.add(
                    NotificationItem(
                        id = "notif_att_${sub.subjectId}",
                        userId = uid,
                        title = "Critical Attendance: ${sub.subjectName}",
                        message = "Current attendance is ${sub.percentage}%. You must attend next ${sub.classesNeeded} classes to avoid shortage.",
                        type = "attendance_warning"
                    )
                )
            } else if (sub.status == "warning") {
                list.add(
                    NotificationItem(
                        id = "notif_att_${sub.subjectId}",
                        userId = uid,
                        title = "Attendance Warning: ${sub.subjectName}",
                        message = "${sub.percentage}% attendance. Please attend next ${sub.classesNeeded} classes to reach safe target.",
                        type = "attendance_warning"
                    )
                )
            }
        }

        // 2. Check Task deadlines
        val tasks = getTasks().filter { it.status != "completed" }
        tasks.forEach { t ->
            if (t.priorityScore >= 70.0) {
                list.add(
                    NotificationItem(
                        id = "notif_tsk_${t.id}",
                        userId = uid,
                        title = "Urgent: ${t.title}",
                        message = "Priority Score: ${t.priorityScore.toInt()} pts. ${t.priorityReason}",
                        type = if (t.taskType.equals("examination", true)) "exam" else "deadline"
                    )
                )
            }
        }

        // 3. Fallback welcome item if empty
        if (list.isEmpty()) {
            list.add(
                NotificationItem(
                    id = "notif_welcome",
                    userId = uid,
                    title = "AcademiaLife Companion",
                    message = "All academic activities are on track. No pending attendance warnings or overdue submissions.",
                    type = "general"
                )
            )
        }

        return list
    }

    // ==========================================
    // 10. GLOBAL SEARCH
    // ==========================================
    suspend fun globalSearch(query: String): List<SearchResultItem> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase(Locale.ROOT)
        val results = mutableListOf<SearchResultItem>()

        // Search Timetable
        val tt = getTimetable()
        tt.filter { it.subjectName.lowercase().contains(q) || it.classroom.lowercase().contains(q) || it.dayOfWeek.lowercase().contains(q) }
            .forEach {
                results.add(
                    SearchResultItem(
                        id = it.id,
                        category = "Timetable",
                        title = "${it.subjectName} (${it.dayOfWeek})",
                        subtitle = "${it.startTime} - ${it.endTime} in ${it.classroom}",
                        details = it.facultyName
                    )
                )
            }

        // Search Tasks
        val tasks = getTasks()
        tasks.filter { it.title.lowercase().contains(q) || it.subjectName.lowercase().contains(q) || it.description.lowercase().contains(q) }
            .forEach {
                results.add(
                    SearchResultItem(
                        id = it.id,
                        category = "Task",
                        title = it.title,
                        subtitle = "${it.taskType.replaceFirstChar { c -> c.uppercase() }} • Due: ${it.dueDate}",
                        details = "Priority: ${it.priorityScore.toInt()} pts"
                    )
                )
            }

        // Search Subjects
        val subs = getSubjects()
        subs.filter { it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }
            .forEach {
                results.add(
                    SearchResultItem(
                        id = it.id,
                        category = "Subject",
                        title = "${it.name} (${it.code})",
                        subtitle = "Faculty: ${it.faculty}",
                        details = "Target: ${it.targetAttendance}%"
                    )
                )
            }

        // Search Documents
        val docs = getDocuments()
        docs.filter { it.title.lowercase().contains(q) || it.category.lowercase().contains(q) || it.notes.lowercase().contains(q) }
            .forEach {
                results.add(
                    SearchResultItem(
                        id = it.id,
                        category = "Document",
                        title = it.title,
                        subtitle = "${it.category} • ${it.subjectName}",
                        details = it.notes
                    )
                )
            }

        // Search Marks
        val marks = getAssessments()
        marks.filter { it.assessmentName.lowercase().contains(q) || it.subjectName.lowercase().contains(q) }
            .forEach {
                results.add(
                    SearchResultItem(
                        id = it.id,
                        category = "Assessment",
                        title = "${it.assessmentName} - ${it.subjectName}",
                        subtitle = "Score: ${it.marksObtained}/${it.maximumMarks} (${it.percentage}%)",
                        details = it.assessmentDate
                    )
                )
            }

        return results
    }

    // ==========================================
    // 11. AI ASSISTANT RAG ENGINE
    // ==========================================
    suspend fun answerQuestion(userQuery: String): ChatMessage {
        val queryLower = userQuery.lowercase(Locale.ROOT)
        val timetable = getTimetable()
        val tasks = getTasks()
        val subjects = getSubjects()
        val records = getAttendanceRecords()
        val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records)
        val studySessions = getStudySessions()
        val docs = getDocuments()

        var answer = ""
        var sourceText = ""

        if (queryLower.contains("class") || queryLower.contains("timetable") || queryLower.contains("schedule") || queryLower.contains("today")) {
            val todayDay = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
            val todays = timetable.filter { it.dayOfWeek.equals(todayDay, ignoreCase = true) }
            if (todays.isNotEmpty()) {
                answer = "You have ${todays.size} class(es) scheduled for today ($todayDay):\n" +
                        todays.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) in ${it.classroom}" }
                sourceText = "Source: Live Timetable ($todayDay)"
            } else {
                answer = "You have no classes scheduled for today ($todayDay) in your timetable."
                sourceText = "Source: Live Timetable"
            }
        } else if (queryLower.contains("attendance") || queryLower.contains("miss") || queryLower.contains("present") || queryLower.contains("bunk")) {
            answer = "Overall attendance is ${attSummary.overallPercentage}% across ${attSummary.totalConducted} classes conducted.\n\n" +
                    attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses}). ${it.explanation}" }
            sourceText = "Source: Firestore Attendance ledger"
        } else if (queryLower.contains("task") || queryLower.contains("assignment") || queryLower.contains("exam") || queryLower.contains("due")) {
            val pending = tasks.filter { it.status != "completed" }
            if (pending.isNotEmpty()) {
                answer = "You have ${pending.size} pending academic deadlines:\n" +
                        pending.take(5).joinToString("\n") { "• ${it.title} [Priority: ${it.priorityScore.toInt()} pts] - ${it.priorityReason}" }
                sourceText = "Source: Tasks & Examination Registry"
            } else {
                answer = "Great news! You have zero pending tasks or assignments. Everything is up to date!"
                sourceText = "Source: Tasks Registry"
            }
        } else if (queryLower.contains("study") || queryLower.contains("timer") || queryLower.contains("plan")) {
            val upcoming = studySessions.filter { it.status == "scheduled" }
            if (upcoming.isNotEmpty()) {
                answer = "You have ${upcoming.size} planned study block(s):\n" +
                        upcoming.take(3).joinToString("\n") { "• ${it.title} (${it.sessionDate} at ${it.startTime}, ${it.durationMinutes} mins) - ${it.recommendationReason}" }
                sourceText = "Source: Study Planner Sessions"
            } else {
                answer = "You don't have scheduled study sessions yet. Tap 'Auto-Generate Plan' in the Study Planner to build an optimized routine!"
                sourceText = "Source: Study Planner"
            }
        } else if (queryLower.contains("document") || queryLower.contains("notes") || queryLower.contains("syllabus")) {
            if (docs.isNotEmpty()) {
                answer = "You have ${docs.size} academic documents in your Document Vault:\n" +
                        docs.take(4).joinToString("\n") { "• ${it.title} [${it.category}] (${it.subjectName})" }
                sourceText = "Source: Document Vault"
            } else {
                answer = "Your Document Vault is currently empty. You can add syllabus, lecture slides, and past papers from the Vault tab."
                sourceText = "Source: Document Vault"
            }
        } else {
            answer = "Hello! I am your AcademiaLife AI tutor. I monitor your attendance compliance, track your assignment priorities, organize your study timer sessions, and check your timetable. How can I help you today?"
            sourceText = "Source: AcademiaLife Academic Core"
        }

        return ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            role = "assistant",
            content = answer,
            sources = sourceText
        )
    }
}
