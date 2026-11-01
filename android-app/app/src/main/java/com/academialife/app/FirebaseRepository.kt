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

    suspend fun signInWithGoogleCredential(idToken: String): Pair<Boolean, String?> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val res = auth.signInWithCredential(credential).await()
            val user = res.user ?: return Pair(false, "Google authentication returned null user")
            val uid = user.uid
            
            // Check if profile exists, if not initialize
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (!doc.exists()) {
                    val profile = StudentProfile(
                        userId = uid,
                        email = user.email ?: "",
                        fullName = user.displayName?.ifEmpty { "Student" } ?: "Student"
                    )
                    saveProfile(profile)
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRepository", "Profile init error: ${e.message}")
            }
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: e.message ?: "Google Sign-In failed")
        }
    }

    suspend fun loginOrRegisterGoogleUser(email: String, fullName: String, googleId: String): Boolean {
        if (email.isEmpty()) return false
        val safePass = "MyBuddyAuth_" + googleId.take(12).ifEmpty { "Secure2026" }
        
        // 1. Try to sign in with standard email credentials
        val (signInOk, _) = signInEmail(email, safePass)
        if (signInOk) return true

        // 2. Try to register as a new verified user
        val (regOk, _) = registerEmail(email, safePass, fullName)
        if (regOk) return true

        // 3. Fallback: anonymous or current auth session + update local profile
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            val uid = getCurrentUserId()
            val profile = StudentProfile(
                userId = uid,
                email = email,
                fullName = fullName.ifEmpty { "Student" }
            )
            saveProfile(profile)
            true
        } catch (_: Exception) {
            true // Allow student to proceed into dashboard with guest fallback
        }
    }

    fun isUserSignedIn(): Boolean = auth.currentUser != null

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
            if (status == "completed") {
                addXP(50, "Completed task")
            }
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
                    title = "MyBuddy Companion",
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
    // 11. DOST — MULTILINGUAL AI ACADEMIC TUTOR
    // ==========================================
    suspend fun answerQuestion(userQuery: String, language: String = "English"): ChatMessage {
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

        // Multi-language keyword detectors
        val isTimetableQuery = queryLower.contains("class") || queryLower.contains("timetable") ||
                queryLower.contains("schedule") || queryLower.contains("today") ||
                queryLower.contains("कक्षा") || queryLower.contains("टाइमटेबल") || queryLower.contains("आज") ||
                queryLower.contains("ತರಗತಿ") || queryLower.contains("తరగతి") || queryLower.contains("வகுப்பு") ||
                queryLower.contains("तासिका") || queryLower.contains("clase") || queryLower.contains("horario")

        val isAttendanceQuery = queryLower.contains("attendance") || queryLower.contains("miss") ||
                queryLower.contains("present") || queryLower.contains("bunk") ||
                queryLower.contains("उपस्थिति") || queryLower.contains("हाजिरी") || queryLower.contains("ಹಾಜರಾತಿ") ||
                queryLower.contains("హాజరు") || queryLower.contains("வருகை") || queryLower.contains("हजेरी") ||
                queryLower.contains("asistencia")

        val isTasksQuery = queryLower.contains("task") || queryLower.contains("assignment") ||
                queryLower.contains("exam") || queryLower.contains("due") || queryLower.contains("deadline") ||
                queryLower.contains("कार्य") || queryLower.contains("असाइनमेंट") || queryLower.contains("परीक्षा") ||
                queryLower.contains("ಕಾರ್ಯ") || queryLower.contains("పరీక్ష") || queryLower.contains("பணி") ||
                queryLower.contains("tarea") || queryLower.contains("examen")

        val isStudyQuery = queryLower.contains("study") || queryLower.contains("timer") ||
                queryLower.contains("plan") || queryLower.contains("routine") ||
                queryLower.contains("पढ़ाई") || queryLower.contains("अध्ययन") || queryLower.contains("ಓದು") ||
                queryLower.contains("చదువు") || queryLower.contains("படிப்பு") || queryLower.contains("estudio")

        val isDocQuery = queryLower.contains("document") || queryLower.contains("notes") ||
                queryLower.contains("syllabus") || queryLower.contains("नोट्स") || queryLower.contains("दस्तऐवज") ||
                queryLower.contains("nota") || queryLower.contains("apuntes")

        val todayDay = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
        val todaysClasses = timetable.filter { it.dayOfWeek.equals(todayDay, ignoreCase = true) }

        val isWaterCommand = queryLower.contains("water") || queryLower.contains("pani") || queryLower.contains("पानी") || queryLower.contains("ನೀರು") || queryLower.contains("నీరు") || queryLower.contains("agua")
        val isAddTaskCommand = queryLower.startsWith("add task ") || queryLower.startsWith("create task ") || queryLower.startsWith("new task ")
        val isProgressQuery = queryLower.contains("progress") || queryLower.contains("streak") || queryLower.contains("xp") || queryLower.contains("level") || queryLower.contains("badge") || queryLower.contains("rank")
        val isStrategyQuery = queryLower.contains("advice") || queryLower.contains("strategy") || queryLower.contains("what should i do") || queryLower.contains("suggest") || queryLower.contains("tips") || queryLower.contains("रणनीति") || queryLower.contains("सलाह")
        val isDeadlockQuery = queryLower.contains("deadlock")
        val isNormalizationQuery = queryLower.contains("normalization") || queryLower.contains("bcnf") || queryLower.contains("normal form")
        val isStudyTipsQuery = queryLower.contains("study tip") || queryLower.contains("how to study") || queryLower.contains("feynman") || queryLower.contains("active recall")

        if (isWaterCommand) {
            val newGlasses = incrementWater()
            answer = "💧 Done, Dost! I've logged 1 glass of water for you. Total today: $newGlasses / 8 glasses (+15 XP awarded)!\n\nStaying hydrated boosts mental focus by up to 20% during study sessions."
            sourceText = "Action: Hydration Tracker"
        } else if (isAddTaskCommand) {
            val taskTitle = userQuery.substringAfter("task").trim().ifEmpty { "New Coursework Assignment" }
            val sdfDue = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 2 * 86400000L))
            val newTask = Task(
                title = taskTitle,
                dueDate = sdfDue,
                taskType = "assignment",
                priorityScore = 75.0,
                priorityReason = "Added via Dost AI Assistant",
                status = "pending"
            )
            addTask(newTask)
            addXP(25, "Added task via Dost AI")
            answer = "🎯 Task added, Dost!\n\n• Title: '$taskTitle'\n• Due Date: $sdfDue\n• Smart Priority Score: 75 pts\n\n+25 XP added to your streak! You can view it in the Planner tab."
            sourceText = "Action: Planner Task Added"
        } else if (isProgressQuery) {
            val g = getGamification()
            val w = getTodayWellness()
            answer = "⚡ Here is your real-time MyBuddy progress, Dost:\n\n" +
                    "• Current Rank: Level ${g.level} • ${g.levelTitle} 🚀\n" +
                    "• XP Points: ${g.xpPoints} XP (${g.level * 400 - g.xpPoints} XP to next level)\n" +
                    "• Study Streak: 🔥 ${g.streakDays} Days\n" +
                    "• Hydration: 💧 ${w.waterGlasses}/8 glasses\n" +
                    "• Unlocked Badges: ${g.unlockedBadges.joinToString(", ")}"
            sourceText = "Source: Gamification & Profile Ledger"
        } else if (isStrategyQuery) {
            val insight = StudentAIEngine.runProactiveAnalysis()
            answer = "${insight.title}\n\n${insight.message}\n\n💡 Recommended Action: Tap '${insight.suggestedAction}' in the app to execute today's target!"
            sourceText = "Source: Student AI Proactive Engine"
        } else if (isDeadlockQuery) {
            answer = "📚 Operating Systems — Deadlock Concept:\n\n" +
                    "A deadlock happens when processes are permanently blocked waiting for resources held by each other.\n\n" +
                    "4 Necessary Coffman Conditions:\n" +
                    "1. Mutual Exclusion (non-shareable resources)\n" +
                    "2. Hold & Wait (holding 1 resource while requesting another)\n" +
                    "3. No Preemption (resources cannot be forcibly taken)\n" +
                    "4. Circular Wait (P1 waiting for P2, P2 for P3... Pn for P1)\n\n" +
                    "💡 Solution: Use Banker's Algorithm (Safe state check) or resource ordering to break circular wait!"
            sourceText = "Subject: Operating Systems Tutoring"
        } else if (isNormalizationQuery) {
            answer = "📚 DBMS — Normalization Quick Summary:\n\n" +
                    "• 1NF: All attribute values must be atomic (no multi-valued/composite attributes).\n" +
                    "• 2NF: Must be in 1NF + No partial dependencies (every non-prime attribute fully depends on Candidate Key).\n" +
                    "• 3NF: Must be in 2NF + No transitive dependencies (if X -> Y, X must be superkey or Y must be prime).\n" +
                    "• BCNF: Stricter than 3NF. For every functional dependency X -> Y, X MUST be a Super Key!"
            sourceText = "Subject: Database Management Systems"
        } else if (isStudyTipsQuery) {
            answer = "🚀 Top Student Study Strategies:\n\n" +
                    "1. Active Recall: Test yourself with flashcards or question prompts instead of passive re-reading.\n" +
                    "2. Feynman Technique: Explain the concept simply in plain words as if teaching a beginner. Wherever you get stuck, re-read that part.\n" +
                    "3. 25/5 Pomodoro: 25 minutes of 100% phone-free focus followed by a 5-minute break.\n" +
                    "4. Spaced Repetition: Review notes at day 1, day 3, day 7, and day 14 to lock information into long-term memory."
            sourceText = "Dost Academic Advice"
        } else if (isTimetableQuery) {
            when (language) {
                "Hindi" -> {
                    answer = if (todaysClasses.isNotEmpty()) {
                        "नमस्ते दोस्त! आज ($todayDay) आपकी ${todaysClasses.size} कक्षाएं हैं:\n" +
                                todaysClasses.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) [${it.classroom}]" }
                    } else {
                        "नमस्ते दोस्त! आज ($todayDay) आपके टाइमटेबल में कोई कक्षा निर्धारित नहीं है। आप अध्ययन कर सकते हैं!"
                    }
                    sourceText = "स्रोत: लाइव टाइमटेबल"
                }
                "Hinglish" -> {
                    answer = if (todaysClasses.isNotEmpty()) {
                        "Hey Dost! Aaj ($todayDay) aapki ${todaysClasses.size} classes hain:\n" +
                                todaysClasses.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) in ${it.classroom}" }
                    } else {
                        "Hey Dost! Aaj ($todayDay) timetable mein koi class scheduled nahi hai. Self-study kar sakte ho!"
                    }
                    sourceText = "Source: Live Timetable"
                }
                "Kannada" -> {
                    answer = if (todaysClasses.isNotEmpty()) {
                        "ನಮಸ್ಕಾರ ದೋಸ್ತ್! ಇಂದು ($todayDay) ನಿಮಗಾಗಿ ${todaysClasses.size} ತರಗತಿಗಳು ನಿಗದಿಯಾಗಿವೆ:\n" +
                                todaysClasses.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) [${it.classroom}]" }
                    } else {
                        "ನಮಸ್ಕಾರ ದೋಸ್ತ್! ಇಂದು ($todayDay) ಯಾವುದೇ ತರಗತಿಗಳು ನಿಗದಿಯಾಗಿಲ್ಲ."
                    }
                    sourceText = "ಮೂಲ: ಲೈವ್ ವೇಳಾಪಟ್ಟಿ"
                }
                "Telugu" -> {
                    answer = if (todaysClasses.isNotEmpty()) {
                        "హలో దోస్త్! ఈరోజు ($todayDay) మీకు ${todaysClasses.size} తరగతులు ఉన్నాయి:\n" +
                                todaysClasses.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) [${it.classroom}]" }
                    } else {
                        "హలో దోస్త్! ఈరోజు మీకు ఎలాంటి తరగతులు లేవు."
                    }
                    sourceText = "మూలం: లైవ్ టైమ్‌టేబుల్"
                }
                "Tamil" -> {
                    answer = if (todaysClasses.isNotEmpty()) {
                        "வணக்கம் தோழா! இன்று ($todayDay) உங்களுக்கு ${todaysClasses.size} வகுப்புகள் உள்ளன:\n" +
                                todaysClasses.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) [${it.classroom}]" }
                    } else {
                        "வணக்கம் தோழா! இன்று உங்களுக்கு வகுப்புகள் ஏதுமில்லை."
                    }
                    sourceText = "மூலம்: நேரலை கால அட்டவணை"
                }
                "Marathi" -> {
                    answer = if (todaysClasses.isNotEmpty()) {
                        "नमस्कार मित्रा! आज ($todayDay) तुमच्या ${todaysClasses.size} तासिका आहेत:\n" +
                                todaysClasses.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) [${it.classroom}]" }
                    } else {
                        "नमस्कार मित्रा! आज कोणत्याही तासिका ठरवलेल्या नाहीत."
                    }
                    sourceText = "स्रोत: वेळापत्रक"
                }
                "Spanish" -> {
                    answer = if (todaysClasses.isNotEmpty()) {
                        "¡Hola amigo! Tienes ${todaysClasses.size} clase(s) programadas para hoy ($todayDay):\n" +
                                todaysClasses.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) en ${it.classroom}" }
                    } else {
                        "¡Hola amigo! No tienes clases programadas para hoy ($todayDay)."
                    }
                    sourceText = "Fuente: Horario en vivo"
                }
                else -> {
                    answer = if (todaysClasses.isNotEmpty()) {
                        "Hey Dost! You have ${todaysClasses.size} class(es) scheduled for today ($todayDay):\n" +
                                todaysClasses.joinToString("\n") { "• ${it.subjectName} (${it.startTime} - ${it.endTime}) in ${it.classroom}" }
                    } else {
                        "Hey Dost! You have no classes scheduled for today ($todayDay). A great day to study or relax!"
                    }
                    sourceText = "Source: Live Timetable ($todayDay)"
                }
            }
        } else if (isAttendanceQuery) {
            when (language) {
                "Hindi" -> {
                    answer = "दोस्त, आपकी कुल उपस्थिति ${attSummary.overallPercentage}% है (${attSummary.totalConducted} कुल कक्षाओं में से)।\n\n" +
                            attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses}). ${it.explanation}" }
                    sourceText = "स्रोत: उपस्थिति रजिस्टर"
                }
                "Hinglish" -> {
                    answer = "Dost, aapka overall attendance ${attSummary.overallPercentage}% hai across ${attSummary.totalConducted} conducted classes.\n\n" +
                            attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses}). ${it.explanation}" }
                    sourceText = "Source: Attendance Register"
                }
                "Kannada" -> {
                    answer = "ದೋಸ್ತ್, ನಿಮ್ಮ ಒಟ್ಟು ಹಾಜರಾತಿ ${attSummary.overallPercentage}% ಆಗಿದೆ (${attSummary.totalConducted} ತರಗತಿಗಳಲ್ಲಿ).\n\n" +
                            attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses})" }
                    sourceText = "ಮೂಲ: ಹಾಜರಾತಿ ಮಾಹಿತಿ"
                }
                "Telugu" -> {
                    answer = "దోస్త్, మీ మొత్తం హాజరు శాతం ${attSummary.overallPercentage}% (${attSummary.totalConducted} తరగతుల్లో).\n\n" +
                            attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses})" }
                    sourceText = "మూలం: హాజరు రికార్డు"
                }
                "Tamil" -> {
                    answer = "தோழா, உங்கள் மொத்த வருகை ${attSummary.overallPercentage}% ஆகும் (${attSummary.totalConducted} வகுப்புகளில்).\n\n" +
                            attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses})" }
                    sourceText = "மூலம்: வருகை பதிவு"
                }
                "Marathi" -> {
                    answer = "मित्रा, तुमची एकूण हजेरी ${attSummary.overallPercentage}% आहे (${attSummary.totalConducted} तासांपैकी).\n\n" +
                            attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses})" }
                    sourceText = "स्रोत: उपस्थिती नोंद"
                }
                "Spanish" -> {
                    answer = "Amigo, tu asistencia general es del ${attSummary.overallPercentage}% en ${attSummary.totalConducted} clases impartidas.\n\n" +
                            attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses})" }
                    sourceText = "Fuente: Registro de Asistencia"
                }
                else -> {
                    answer = "Hey Dost! Overall attendance is ${attSummary.overallPercentage}% across ${attSummary.totalConducted} classes conducted.\n\n" +
                            attSummary.subjectSummaries.joinToString("\n") { "• ${it.subjectName}: ${it.percentage}% (${it.presentCount}/${it.totalClasses}). ${it.explanation}" }
                    sourceText = "Source: Attendance Ledger"
                }
            }
        } else if (isTasksQuery) {
            val pending = tasks.filter { it.status != "completed" }
            when (language) {
                "Hindi" -> {
                    answer = if (pending.isNotEmpty()) {
                        "दोस्त, आपके पास ${pending.size} असाइनमेंट व डेडलाइन लंबित हैं:\n" +
                                pending.take(5).joinToString("\n") { "• ${it.title} [प्राथमिकता: ${it.priorityScore.toInt()} अंक] - ${it.priorityReason}" }
                    } else {
                        "शाबाश दोस्त! आपके सभी असाइनमेंट पूरे हैं। कोई लंबित कार्य नहीं है!"
                    }
                    sourceText = "स्रोत: कार्य और परीक्षा पंजी"
                }
                "Hinglish" -> {
                    answer = if (pending.isNotEmpty()) {
                        "Dost, aapke paas ${pending.size} pending tasks & assignments hain:\n" +
                                pending.take(5).joinToString("\n") { "• ${it.title} [Priority: ${it.priorityScore.toInt()} pts] - ${it.priorityReason}" }
                    } else {
                        "Great job Dost! All tasks are completed. Nothing is pending right now!"
                    }
                    sourceText = "Source: Tasks Registry"
                }
                "Kannada" -> {
                    answer = if (pending.isNotEmpty()) {
                        "ದೋಸ್ತ್, ನಿಮಗೆ ${pending.size} ಬಾಕಿ ಇರುವ ಕಾರ್ಯಗಳು ಇವೆ:\n" +
                                pending.take(5).joinToString("\n") { "• ${it.title} (ಆದ್ಯತೆ: ${it.priorityScore.toInt()} pts)" }
                    } else {
                        "ಅಭಿನಂದನೆಗಳು ದೋಸ್ತ್! ನಿಮ್ಮ ಎಲ್ಲಾ ಕಾರ್ಯಗಳು ಪೂರ್ಣಗೊಂಡಿವೆ!"
                    }
                    sourceText = "ಮೂಲ: ಕಾರ್ಯಗಳ ಪಟ್ಟಿ"
                }
                "Telugu" -> {
                    answer = if (pending.isNotEmpty()) {
                        "దోస్త్, మీ వద్ద ${pending.size} పెండింగ్ టాస్కులు ఉన్నాయి:\n" +
                                pending.take(5).joinToString("\n") { "• ${it.title} (ప్రాధాన్యత: ${it.priorityScore.toInt()} pts)" }
                    } else {
                        "అద్భుతం దోస్త్! మీ అన్ని టాస్కులు పూర్తయ్యాయి!"
                    }
                    sourceText = "మూలం: టాస్క్ రిజిస్ట్రీ"
                }
                "Tamil" -> {
                    answer = if (pending.isNotEmpty()) {
                        "தோழா, உங்களிடம் ${pending.size} நிலுவையிலுள்ள பணிகள் உள்ளன:\n" +
                                pending.take(5).joinToString("\n") { "• ${it.title} [முன்னுரிமை: ${it.priorityScore.toInt()} pts]" }
                    } else {
                        "மிக நன்று தோழா! அனைத்து பணிகளும் முடிந்துவிட்டன!"
                    }
                    sourceText = "மூலம்: பணிகள் பதிவேடு"
                }
                "Marathi" -> {
                    answer = if (pending.isNotEmpty()) {
                        "मित्रा, तुमच्याकडे ${pending.size} कामे प्रलंबित आहेत:\n" +
                                pending.take(5).joinToString("\n") { "• ${it.title} [प्राधान्य: ${it.priorityScore.toInt()} pts]" }
                    } else {
                        "उत्तम मित्रा! तुमची सर्व कामे पूर्ण झाली आहेत!"
                    }
                    sourceText = "स्रोत: कामे नोंदवही"
                }
                "Spanish" -> {
                    answer = if (pending.isNotEmpty()) {
                        "Amigo, tienes ${pending.size} tareas académicas pendientes:\n" +
                                pending.take(5).joinToString("\n") { "• ${it.title} [Prioridad: ${it.priorityScore.toInt()} pts]" }
                    } else {
                        "¡Excelente amigo! Todas tus tareas están completadas."
                    }
                    sourceText = "Fuente: Registro de Tareas"
                }
                else -> {
                    answer = if (pending.isNotEmpty()) {
                        "Hey Dost! You have ${pending.size} pending academic deadlines:\n" +
                                pending.take(5).joinToString("\n") { "• ${it.title} [Priority: ${it.priorityScore.toInt()} pts] - ${it.priorityReason}" }
                    } else {
                        "Great news Dost! You have zero pending tasks or assignments. Everything is up to date!"
                    }
                    sourceText = "Source: Tasks & Examination Registry"
                }
            }
        } else if (isStudyQuery) {
            val upcoming = studySessions.filter { it.status == "scheduled" }
            answer = if (upcoming.isNotEmpty()) {
                "Dost, you have ${upcoming.size} planned study block(s):\n" +
                        upcoming.take(3).joinToString("\n") { "• ${it.title} (${it.sessionDate} at ${it.startTime}, ${it.durationMinutes} mins) - ${it.recommendationReason}" }
            } else {
                "Dost, you don't have scheduled study sessions yet. Tap 'Auto-Generate Plan' in Study Planner to build an optimized routine!"
            }
            sourceText = "Source: Study Planner Sessions"
        } else if (isDocQuery) {
            answer = if (docs.isNotEmpty()) {
                "Dost, you have ${docs.size} academic documents in your Document Vault:\n" +
                        docs.take(4).joinToString("\n") { "• ${it.title} [${it.category}] (${it.subjectName})" }
            } else {
                "Your Document Vault is empty right now. You can add syllabus and notes anytime!"
            }
            sourceText = "Source: Document Vault"
        } else {
            when (language) {
                "Hindi" -> {
                    answer = "नमस्ते! मैं आपका MyBuddy 'दोस्त' AI ट्यूटर हूँ। मैं आपके टाइमटेबल, अटेंडेंस और असाइनमेंट में आपकी सहायता करता हूँ। आप मुझसे कुछ भी पूछ सकते हैं!"
                    sourceText = "स्रोत: MyBuddy AI Core"
                }
                "Hinglish" -> {
                    answer = "Hey Dost! Main aapka MyBuddy AI tutor hoon. Main aapke timetable, attendance compliance aur assignment deadlines ko track karta hoon. Kuch bhi poocho!"
                    sourceText = "Source: MyBuddy AI Core"
                }
                "Kannada" -> {
                    answer = "ನಮಸ್ಕಾರ! ನಾನು ನಿಮ್ಮ MyBuddy 'ದೋಸ್ತ್' AI ಬೋಧಕ. ನಿಮ್ಮ ವೇಳಾಪಟ್ಟಿ, ಹಾಜರಾತಿ ಮತ್ತು ಕಾರ್ಯಗಳ ಬಗ್ಗೆ ನಾನು ಸಹಾಯ ಮಾಡುತ್ತೇನೆ. ಏನು ಬೇಕಾದರೂ ಕೇಳಿ!"
                    sourceText = "ಮೂಲ: MyBuddy AI Core"
                }
                "Telugu" -> {
                    answer = "హలో! నేను మీ MyBuddy 'దోస్త్' AI ట్యూటర్‌ని. మీ టైమ్‌టేబుల్, హాజరు మరియు టాస్కుల వివరాలు చెప్పగలను. నన్ను ఏదైనా అడగండి!"
                    sourceText = "మూలం: MyBuddy AI Core"
                }
                "Tamil" -> {
                    answer = "வணக்கம்! நான் உங்கள் MyBuddy 'தோழா' AI வழிகாட்டி. கால அட்டவணை, வருகை மற்றும் பணிகள் பற்றி என்னிடம் எதையும் கேட்கலாம்!"
                    sourceText = "மூலம்: MyBuddy AI Core"
                }
                "Marathi" -> {
                    answer = "नमस्कार! मी तुमचा MyBuddy 'दोस्त' AI ट्यूटर आहे. वेळापत्रक, हजेरी आणि असाइनमेंटबद्दल काहीही विचारा!"
                    sourceText = "स्रोत: MyBuddy AI Core"
                }
                "Spanish" -> {
                    answer = "¡Hola! Soy tu tutor de IA MyBuddy 'Dost'. Te ayudo con tu horario, asistencia y tareas pendientes. ¡Pregúntame lo que necesites!"
                    sourceText = "Fuente: MyBuddy AI Core"
                }
                else -> {
                    answer = "Hey! I am your MyBuddy AI tutor, 'Dost'. I track your timetable, attendance compliance, assignment priorities, and study plans. How can I help you today?"
                    sourceText = "Source: MyBuddy Academic Core"
                }
            }
        }

        return ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            role = "assistant",
            content = answer,
            sources = sourceText
        )
    }

    // ==========================================
    // 12. DOPAMINE GAMIFICATION & STREAKS
    // ==========================================
    private var cachedGamification = GamificationProfile()

    fun getGamification(): GamificationProfile {
        return cachedGamification
    }

    fun addXP(points: Int, reason: String): GamificationProfile {
        val newXP = cachedGamification.xpPoints + points
        val newLevel = when {
            newXP >= 2500 -> 5
            newXP >= 1500 -> 4
            newXP >= 800 -> 3
            newXP >= 300 -> 2
            else -> 1
        }
        val newTitle = when (newLevel) {
            5 -> "Academic Titan ⚡"
            4 -> "Dean's List 🌟"
            3 -> "Prodigy 🚀"
            2 -> "Scholar 🎓"
            else -> "Freshman 🌱"
        }
        cachedGamification = cachedGamification.copy(
            xpPoints = newXP,
            level = newLevel,
            levelTitle = newTitle
        )
        return cachedGamification
    }

    fun getAvailableBadges(): List<BadgeItem> {
        val g = cachedGamification
        return listOf(
            BadgeItem("b1", "First Step", "Complete your first task in MyBuddy", "🎯", g.completedTasksCount >= 1, 50),
            BadgeItem("b2", "Focus Master", "Complete a 25-minute Pomodoro focus block", "⏱️", g.focusMinutesTotal >= 25, 100),
            BadgeItem("b3", "Streak Champion", "Maintain a 3-day consecutive study streak", "🔥", g.streakDays >= 3, 150),
            BadgeItem("b4", "Assignment Crusher", "Finish 5 assignments before the deadline", "📚", g.completedTasksCount >= 5, 200),
            BadgeItem("b5", "Hydration Hero", "Reach your 8 glasses of water goal in a day", "💧", true, 75),
            BadgeItem("b6", "Consistency King", "Log attendance across all subjects", "👑", true, 120)
        )
    }

    // ==========================================
    // 13. STUDENT FINANCE TRACKER
    // ==========================================
    private val cachedExpenses = mutableListOf(
        ExpenseItem("exp_1", "", "Campus Cafeteria Lunch", 140.0, "Food", "Today", System.currentTimeMillis()),
        ExpenseItem("exp_2", "", "Metro Rail Pass", 250.0, "Travel", "Yesterday", System.currentTimeMillis() - 86400000L),
        ExpenseItem("exp_3", "", "Operating Systems Textbook", 450.0, "Books", "2 days ago", System.currentTimeMillis() - 172800000L)
    )
    private var monthlyBudgetAmount = 6000.0

    fun getExpenses(): List<ExpenseItem> = cachedExpenses.toList()

    fun addExpense(title: String, amount: Double, category: String): ExpenseItem {
        val item = ExpenseItem(
            id = "exp_" + UUID.randomUUID().toString().take(8),
            title = title,
            amount = amount,
            category = category,
            date = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
            timestamp = System.currentTimeMillis()
        )
        cachedExpenses.add(0, item)
        addXP(20, "Logged an expense")
        return item
    }

    fun deleteExpense(id: String) {
        cachedExpenses.removeAll { it.id == id }
    }

    fun getBudget(): FinanceBudget {
        val totalSpent = cachedExpenses.sumOf { it.amount }
        return FinanceBudget(monthlyBudget = monthlyBudgetAmount, spentThisMonth = totalSpent)
    }

    fun setBudget(newBudget: Double) {
        monthlyBudgetAmount = newBudget
    }

    // ==========================================
    // 14. STUDENT WELLNESS & DAILY HABITS
    // ==========================================
    private var todayWellness = WellnessEntry(
        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        mood = "Motivated",
        waterGlasses = 5,
        sleepHours = 7.5,
        journalNote = "Feeling energized for today's lectures!"
    )

    fun getTodayWellness(): WellnessEntry = todayWellness

    fun saveWellness(mood: String, water: Int, sleep: Double, note: String): WellnessEntry {
        todayWellness = todayWellness.copy(
            mood = mood,
            waterGlasses = water,
            sleepHours = sleep,
            journalNote = note
        )
        addXP(30, "Updated wellness habits")
        return todayWellness
    }

    fun incrementWater(): Int {
        val newCount = (todayWellness.waterGlasses + 1).coerceAtMost(8)
        todayWellness = todayWellness.copy(waterGlasses = newCount)
        addXP(15, "Drank a glass of water")
        return newCount
    }

    // ==========================================
    // 15. SMART NOTES
    // ==========================================
    private val cachedNotes = mutableListOf(
        StudentNote("note_1", "", "OS Process Scheduling Cheatsheet", "Round Robin uses time quantum. Priority Scheduling can suffer from starvation (fix with aging). Multi-level feedback queues are dynamic.", "Operating Systems", isPinned = true, colorHex = "#7952F2", updatedAt = "Today"),
        StudentNote("note_2", "", "DBMS Normalization Summary", "1NF: Atomic values. 2NF: No partial dependency on candidate key. 3NF: No transitive dependency. BCNF: For every X -> Y, X is super key.", "Database Systems", isPinned = true, colorHex = "#FF6B5E", updatedAt = "Yesterday"),
        StudentNote("note_3", "", "Exam Formulas - Discrete Math", "Graph theory: Handshaking lemma sum of degrees = 2|E|. Euler formula: V - E + F = 2. Pigeonhole principle basics.", "Mathematics", isPinned = false, colorHex = "#4169F5", updatedAt = "3 days ago")
    )

    fun getStudentNotes(): List<StudentNote> = cachedNotes.toList()

    fun saveStudentNote(title: String, content: String, subjectTag: String, colorHex: String = "#7952F2"): StudentNote {
        val note = StudentNote(
            id = "note_" + UUID.randomUUID().toString().take(8),
            title = title,
            content = content,
            subjectTag = subjectTag,
            colorHex = colorHex,
            updatedAt = "Just now"
        )
        cachedNotes.add(0, note)
        addXP(25, "Created a study note")
        return note
    }

    fun deleteStudentNote(id: String) {
        cachedNotes.removeAll { it.id == id }
    }

    fun togglePinStudentNote(id: String) {
        val idx = cachedNotes.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val curr = cachedNotes[idx]
            cachedNotes[idx] = curr.copy(isPinned = !curr.isPinned)
        }
    }
}
