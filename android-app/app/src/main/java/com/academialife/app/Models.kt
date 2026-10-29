package com.academialife.app

data class StudentProfile(
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val college: String = "National Institute of Technology",
    val course: String = "B.Tech Computer Science & Engineering",
    val semester: String = "Semester 6",
    val academicYear: String = "2026-2027",
    val minimumAttendanceTarget: Double = 75.0,
    val preferredStudyHoursPerDay: Double = 4.0,
    val preferredBreakDurationMinutes: Int = 15
)

data class UserPreferences(
    val theme: String = "light_neumorphic",
    val attendanceWarningThreshold: Double = 75.0,
    val soundEnabled: Boolean = true,
    val reminderHoursBeforeDeadline: Int = 24
)

data class Subject(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val code: String = "",
    val faculty: String = "",
    val color: String = "#8175E5",
    val targetAttendance: Double = 75.0
)

data class TimetableEntry(
    val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val subjectColor: String = "#8175E5",
    val dayOfWeek: String = "Monday",
    val startTime: String = "09:00",
    val endTime: String = "10:30",
    val classroom: String = "Hall 101",
    val facultyName: String = "",
    val notes: String = ""
)

data class Task(
    val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val subjectColor: String = "#8175E5",
    val title: String = "",
    val description: String = "",
    val taskType: String = "assignment", // assignment, examination, project, revision
    val dueDate: String = "",
    val estimatedEffortMinutes: Int = 60,
    val priority: String = "medium", // low, medium, high, urgent
    val priorityScore: Double = 50.0,
    val priorityReason: String = "",
    val status: String = "pending", // pending, completed
    val isOverdue: Boolean = false
)

data class AttendanceRecord(
    val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val date: String = "",
    val status: String = "present", // present, absent, leave
    val notes: String = ""
)

data class SubjectAttendanceSummary(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val color: String,
    val totalClasses: Int,
    val presentCount: Int,
    val absentCount: Int,
    val leaveCount: Int,
    val percentage: Double,
    val targetPercentage: Double,
    val status: String, // safe, warning, critical
    val classesNeeded: Int,
    val explanation: String
)

data class OverallAttendanceSummary(
    val overallPercentage: Double,
    val totalConducted: Int,
    val totalPresent: Int,
    val totalAbsent: Int,
    val targetPercentage: Double,
    val isWarning: Boolean,
    val subjectSummaries: List<SubjectAttendanceSummary>
)

data class Assessment(
    val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val assessmentName: String = "",
    val assessmentType: String = "Quiz", // Midterm, Final, Quiz, Lab, Assignment
    val marksObtained: Double = 0.0,
    val maximumMarks: Double = 20.0,
    val percentage: Double = 0.0,
    val assessmentDate: String = "",
    val notes: String = ""
)

data class SubjectPerformanceSummary(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val assessmentCount: Int,
    val averagePercentage: Double,
    val highestPercentage: Double,
    val lowestPercentage: Double
)

data class StudySession(
    val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val title: String = "",
    val sessionDate: String = "",
    val startTime: String = "18:00",
    val endTime: String = "19:30",
    val durationMinutes: Int = 90,
    val status: String = "scheduled", // scheduled, completed, skipped
    val recommendationReason: String = "Self-scheduled study block"
)

data class VaultDocument(
    val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val title: String = "",
    val category: String = "Lecture Notes", // Syllabus, Lecture Notes, Question Papers, Cheatsheets, References
    val fileUrl: String = "",
    val fileSizeKb: Int = 120,
    val notes: String = "",
    val createdAt: String = ""
)

data class NotificationItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "general", // attendance_warning, deadline, exam, study_plan, general
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = "",
    val role: String = "user", // user, assistant
    val content: String = "",
    val sources: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SearchResultItem(
    val id: String,
    val category: String, // Timetable, Task, Attendance, Assessment, Document
    val title: String,
    val subtitle: String,
    val details: String = ""
)
