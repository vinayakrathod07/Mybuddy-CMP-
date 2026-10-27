package com.academialife.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object AcademicCalculators {

    /**
     * Deterministic priority scoring formula (0.0 to 100.0).
     * Components:
     * 1. Base Priority Weight (10 to 40 pts)
     * 2. Task Type Weight (10 to 35 pts)
     * 3. Urgency / Proximity Weight (3 to 35 pts)
     * 4. Effort Factor (2 to 10 pts)
     */
    fun calculatePriorityScore(
        taskType: String,
        priority: String,
        dueDateStr: String,
        effortMinutes: Int,
        isCompleted: Boolean = false
    ): Pair<Double, String> {
        if (isCompleted) {
            return Pair(0.0, "Task is completed")
        }

        val pScore = when (priority.lowercase(Locale.ROOT)) {
            "urgent" -> 40.0
            "high" -> 30.0
            "medium" -> 20.0
            else -> 10.0
        }

        val tScore = when (taskType.lowercase(Locale.ROOT)) {
            "examination" -> 35.0
            "assignment" -> 25.0
            "project" -> 20.0
            "revision" -> 15.0
            else -> 10.0
        }

        var hoursLeft = 48.0
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val date = sdf.parse(dueDateStr) ?: Date()
            val diffMs = date.time - System.currentTimeMillis()
            hoursLeft = diffMs / (1000.0 * 3600.0)
        } catch (_: Exception) {}

        val (uScore, urgencyText) = when {
            hoursLeft < 0 -> Pair(35.0, "OVERDUE by ${kotlin.math.abs(hoursLeft.toInt())}h")
            hoursLeft <= 24 -> Pair(30.0, "due in ${max(1, hoursLeft.toInt())}h")
            hoursLeft <= 72 -> Pair(22.0, "due in ${(hoursLeft / 24).toInt()}d")
            hoursLeft <= 168 -> Pair(15.0, "due in ${(hoursLeft / 24).toInt()}d")
            else -> Pair(5.0, "due in ${(hoursLeft / 24).toInt()}d")
        }

        val (eScore, effortText) = when {
            effortMinutes >= 180 -> Pair(10.0, "${effortMinutes / 60}h effort")
            effortMinutes >= 120 -> Pair(7.0, "2h effort")
            effortMinutes >= 60 -> Pair(4.0, "1h effort")
            else -> Pair(2.0, "${effortMinutes}m effort")
        }

        val rawTotal = pScore + tScore + uScore + eScore
        val normalized = min(100.0, round((rawTotal / 120.0) * 1000.0) / 10.0)
        val reason = "${taskType.replaceFirstChar { it.uppercase() }} ($priority priority, $urgencyText, $effortText)"

        return Pair(normalized, reason)
    }

    /**
     * Minimum consecutive attended classes needed to reach targetPct.
     * Formula: (P + x) / (C + x) >= target
     * => x = ceil((target * C - P) / (1 - target))
     */
    fun calculateClassesNeeded(present: Int, total: Int, targetPct: Double): Int {
        if (total == 0) return 0
        val currentPct = (present.toDouble() / total.toDouble()) * 100.0
        if (currentPct >= targetPct) return 0

        val tRatio = targetPct / 100.0
        if (tRatio >= 1.0) return 0

        val numerator = (tRatio * total) - present
        val denominator = 1.0 - tRatio
        val needed = ceil(numerator / denominator).toInt()
        return max(0, needed)
    }

    /**
     * Compute full attendance compliance summary
     */
    fun computeAttendanceSummary(
        subjects: List<Subject>,
        records: List<AttendanceRecord>,
        minThreshold: Double = 75.0
    ): OverallAttendanceSummary {
        val subjectSummaries = subjects.map { sub ->
            val subRecs = records.filter { it.subjectId == sub.id }
            val total = subRecs.size
            val present = subRecs.count { it.status == "present" }
            val absent = subRecs.count { it.status == "absent" }
            val leave = subRecs.count { it.status == "leave" }
            val pct = if (total > 0) round((present.toDouble() / total.toDouble()) * 1000.0) / 10.0 else 100.0
            val target = if (sub.targetAttendance > 0) sub.targetAttendance else minThreshold

            val (statusLabel, classesNeeded, explanation) = when {
                total == 0 -> Triple("safe", 0, "No classes logged yet for this subject.")
                pct >= target -> {
                    val canMiss = floor((100.0 * present - target * total) / target).toInt()
                    val exp = if (canMiss > 0) {
                        "Attendance is healthy ($pct%). You can miss up to $canMiss class${if (canMiss > 1) "es" else ""} safely."
                    } else {
                        "Attendance meets the target ($pct%). Do not miss the next class."
                    }
                    Triple("safe", 0, exp)
                }
                pct >= target - 10.0 -> {
                    val needed = calculateClassesNeeded(present, total, target)
                    Triple("warning", needed, "Attendance warning ($pct%). Attend next $needed consecutive classes to reach $target%.")
                }
                else -> {
                    val needed = calculateClassesNeeded(present, total, target)
                    Triple("critical", needed, "Critical shortage ($pct%). Must attend next $needed consecutive classes.")
                }
            }

            SubjectAttendanceSummary(
                subjectId = sub.id,
                subjectName = sub.name,
                subjectCode = sub.code,
                color = sub.color,
                totalClasses = total,
                presentCount = present,
                absentCount = absent,
                leaveCount = leave,
                percentage = pct,
                targetPercentage = target,
                status = statusLabel,
                classesNeeded = classesNeeded,
                explanation = explanation
            )
        }

        val totalConducted = records.size
        val totalPresent = records.count { it.status == "present" }
        val totalAbsent = records.count { it.status == "absent" }
        val overallPct = if (totalConducted > 0) round((totalPresent.toDouble() / totalConducted.toDouble()) * 1000.0) / 10.0 else 100.0
        val isWarning = totalConducted > 0 && overallPct < minThreshold

        return OverallAttendanceSummary(
            overallPercentage = overallPct,
            totalConducted = totalConducted,
            totalPresent = totalPresent,
            totalAbsent = totalAbsent,
            targetPercentage = minThreshold,
            isWarning = isWarning,
            subjectSummaries = subjectSummaries
        )
    }

    /**
     * Check if a timetable entry overlaps with existing classes on the same day
     */
    fun checkTimetableConflict(
        existing: List<TimetableEntry>,
        dayOfWeek: String,
        startTime: String,
        endTime: String,
        excludeId: String? = null
    ): TimetableEntry? {
        val sameDay = existing.filter {
            it.dayOfWeek.equals(dayOfWeek, ignoreCase = true) && it.id != excludeId
        }
        return sameDay.firstOrNull { it.startTime < endTime && it.endTime > startTime }
    }
}
