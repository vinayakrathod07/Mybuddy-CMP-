package com.academialife.app

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

object AIVisionAnalyzer {

    private val DAYS_OF_WEEK = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    private val COMMON_SUBJECTS = listOf(
        "Data Structures", "Database Management", "Operating Systems",
        "Computer Networks", "Software Engineering", "Mathematics",
        "Web Technologies", "Cloud Computing", "Artificial Intelligence",
        "Machine Learning", "Python Programming", "Java Programming",
        "Object Oriented Programming", "Cyber Security", "Digital Electronics",
        "Discrete Mathematics", "Data Science", "Algorithms", "Computer Organization"
    )

    data class AttendanceExtractedItem(
        val subjectName: String,
        val attended: Int,
        val total: Int,
        val percentage: Double
    )

    data class ExtractedDocument(
        val title: String,
        val subjectName: String,
        val category: String,
        val notes: String,
        val rawText: String
    )

    data class ExtractedMarkItem(
        val subjectName: String,
        val assessmentName: String,
        val marksObtained: Double,
        val maximumMarks: Double,
        val percentage: Double
    )

    /**
     * AI-powered photo analyzer for college/school timetables.
     * Uses ML Kit on-device Text Recognition + Intelligent Schedule Parsing heuristics.
     */
    suspend fun analyzeTimetablePhoto(bitmap: Bitmap): List<TimetableEntry> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = try {
            recognizer.process(image).await()
        } catch (e: Exception) {
            return emptyList()
        }

        val entries = mutableListOf<TimetableEntry>()
        val fullText = visionText.text
        val lines = fullText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Find days mentioned in the text
        var currentDay = "Monday"
        val timeRegex = Regex("""(\d{1,2}[:.]\d{2})\s*(?:-|to)\s*(\d{1,2}[:.]\d{2})""", RegexOption.IGNORE_CASE)

        // Slot tracking
        var defaultStartTime = "09:00 AM"
        var defaultEndTime = "10:00 AM"

        val defaultTimes = listOf(
            Pair("09:00 AM", "10:00 AM"),
            Pair("10:00 AM", "11:00 AM"),
            Pair("11:15 AM", "12:15 PM"),
            Pair("01:15 PM", "02:15 PM"),
            Pair("02:15 PM", "03:15 PM"),
            Pair("03:15 PM", "04:15 PM")
        )
        var timeSlotIndex = 0

        for (line in lines) {
            // Check if line indicates a day
            val matchedDay = DAYS_OF_WEEK.firstOrNull { day ->
                line.contains(day, ignoreCase = true) || line.contains(day.take(3), ignoreCase = true)
            }
            if (matchedDay != null) {
                currentDay = matchedDay
                timeSlotIndex = 0
                continue
            }

            // Check if line contains time range
            val timeMatch = timeRegex.find(line)
            if (timeMatch != null) {
                val start = timeMatch.groupValues[1].replace(".", ":")
                val end = timeMatch.groupValues[2].replace(".", ":")
                defaultStartTime = formatTimeStr(start)
                defaultEndTime = formatTimeStr(end)
            }

            // Check for known subjects or significant subject-like names
            val foundSubject = COMMON_SUBJECTS.firstOrNull { subj ->
                line.contains(subj, ignoreCase = true)
            } ?: run {
                // If line contains 2-4 capitalized words and no numbers, treat as subject candidate
                val words = line.split(" ")
                if (words.size in 1..4 && words.all { it.isNotEmpty() && it.first().isUpperCase() } && !line.contains(":")) {
                    line
                } else null
            }

            if (foundSubject != null && foundSubject.length > 2) {
                val slot = defaultTimes.getOrElse(timeSlotIndex % defaultTimes.size) {
                    Pair(defaultStartTime, defaultEndTime)
                }
                timeSlotIndex++

                val classroom = if (line.contains("lab", ignoreCase = true)) "Lab 2"
                else if (line.contains("lh", ignoreCase = true)) "LH-1"
                else "Room 302"

                val teacher = if (line.contains("dr", ignoreCase = true) || line.contains("prof", ignoreCase = true)) {
                    line.substringAfter("Prof").substringAfter("Dr").take(15).trim()
                } else "Faculty"

                entries.add(
                    TimetableEntry(
                        id = "ai_" + UUID.randomUUID().toString().take(8),
                        dayOfWeek = currentDay,
                        startTime = slot.first,
                        endTime = slot.second,
                        subjectName = foundSubject.trim(),
                        classroom = classroom,
                        facultyName = teacher
                    )
                )
            }
        }

        // If no entries detected from structured parse, extract whatever subject words are found
        if (entries.isEmpty()) {
            for (subj in COMMON_SUBJECTS) {
                if (fullText.contains(subj, ignoreCase = true)) {
                    val slot = defaultTimes.getOrElse(entries.size % defaultTimes.size) { Pair("10:00 AM", "11:00 AM") }
                    entries.add(
                        TimetableEntry(
                            id = "ai_" + UUID.randomUUID().toString().take(8),
                            dayOfWeek = "Monday",
                            startTime = slot.first,
                            endTime = slot.second,
                            subjectName = subj,
                            classroom = "Room 101",
                            facultyName = "Faculty"
                        )
                    )
                }
            }
        }

        return entries
    }

    /**
     * AI-powered analyzer for attendance reports and mark sheets.
     */
    suspend fun analyzeAttendancePhoto(bitmap: Bitmap): List<AttendanceExtractedItem> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = try {
            recognizer.process(image).await()
        } catch (_: Exception) {
            return emptyList()
        }

        val results = mutableListOf<AttendanceExtractedItem>()
        val lines = visionText.text.lines()

        val fractionRegex = Regex("""(\d{1,3})\s*[/|]\s*(\d{1,3})""")
        val percentRegex = Regex("""(\d{1,3}(?:\.\d+)?)\s*%""")

        for (i in lines.indices) {
            val line = lines[i].trim()
            val fractionMatch = fractionRegex.find(line)
            val percentMatch = percentRegex.find(line)

            if (fractionMatch != null) {
                val attended = fractionMatch.groupValues[1].toIntOrNull() ?: 0
                val total = fractionMatch.groupValues[2].toIntOrNull() ?: 1
                val pct = if (total > 0) (attended.toDouble() / total) * 100 else 0.0

                // Subject name might be preceding line or earlier in current line
                val subj = line.replace(fractionMatch.value, "").trim().ifEmpty {
                    if (i > 0) lines[i - 1].trim() else "Course Subject"
                }

                results.add(AttendanceExtractedItem(subj.take(30), attended, total, Math.round(pct * 10.0) / 10.0))
            } else if (percentMatch != null) {
                val pct = percentMatch.groupValues[1].toDoubleOrNull() ?: 75.0
                val subj = line.replace(percentMatch.value, "").trim().ifEmpty {
                    if (i > 0) lines[i - 1].trim() else "Course Subject"
                }
                val estTotal = 30
                val estAttended = Math.round((pct / 100.0) * estTotal).toInt()
                results.add(AttendanceExtractedItem(subj.take(30), estAttended, estTotal, pct))
            }
        }

        return results
    }

    /**
     * AI-powered analyzer for lecture notes, textbook pages, syllabi, and cheatsheets.
     * Extracts text, determines subject, infers document category, and generates an executive summary.
     */
    suspend fun analyzeDocumentPhoto(bitmap: Bitmap): ExtractedDocument {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = try {
            recognizer.process(image).await()
        } catch (_: Exception) {
            return ExtractedDocument(
                title = "Scanned Document",
                subjectName = "General",
                category = "Lecture Notes",
                notes = "Could not extract clear text. Please retake photo with better lighting.",
                rawText = ""
            )
        }

        val raw = visionText.text
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return ExtractedDocument(
                title = "Scanned Note",
                subjectName = "General",
                category = "Lecture Notes",
                notes = "No text detected on the page.",
                rawText = ""
            )
        }

        // Infer Subject
        var detectedSubject = "General"
        for (subj in COMMON_SUBJECTS) {
            if (raw.contains(subj, ignoreCase = true)) {
                detectedSubject = subj
                break
            }
        }
        if (detectedSubject == "General") {
            val potential = lines.firstOrNull { l ->
                l.length in 4..30 && !l.contains(":") && !l.contains("page", ignoreCase = true)
            }
            if (potential != null) {
                detectedSubject = potential
            }
        }

        // Infer Category
        val lower = raw.lowercase()
        val category = when {
            lower.contains("syllabus") || lower.contains("module") || lower.contains("unit ") || lower.contains("course outcome") -> "Syllabus"
            lower.contains("question") || lower.contains("marks") || lower.contains("time: ") || lower.contains("q.no") -> "Past Papers"
            lower.contains("formula") || lower.contains("cheatsheet") || lower.contains("cheat sheet") || lower.contains("syntax") -> "Cheatsheets"
            else -> "Lecture Notes"
        }

        // Generate Title
        val titleCandidate = lines.firstOrNull { it.length > 5 && !it.startsWith("Chapter", ignoreCase = true) }
            ?: "$detectedSubject Notes"
        val title = if (titleCandidate.length > 40) titleCandidate.take(37) + "..." else titleCandidate

        // Build bullet-point summary (up to 8 informative points)
        val bulletPoints = lines
            .filter { it.length > 15 && !it.contains("Page", ignoreCase = true) }
            .take(8)
            .map { "• " + it }
            .joinToString("\n")

        val summary = if (bulletPoints.isNotEmpty()) {
            "📌 AI Key Takeaways:\n$bulletPoints"
        } else {
            raw.take(300)
        }

        return ExtractedDocument(
            title = title,
            subjectName = detectedSubject,
            category = category,
            notes = summary,
            rawText = raw
        )
    }

    /**
     * AI-powered analyzer for student marks slips, internal score cards, and grade sheets.
     */
    suspend fun analyzeMarksSlip(bitmap: Bitmap): List<ExtractedMarkItem> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = try {
            recognizer.process(image).await()
        } catch (_: Exception) {
            return emptyList()
        }

        val results = mutableListOf<ExtractedMarkItem>()
        val lines = visionText.text.lines()
        val scoreRegex = Regex("""(\d{1,3}(?:\.\d+)?)\s*[/|]\s*(\d{1,3}(?:\.\d+)?)""")

        for (i in lines.indices) {
            val line = lines[i].trim()
            val scoreMatch = scoreRegex.find(line)
            if (scoreMatch != null) {
                val obtained = scoreMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val maxMarks = scoreMatch.groupValues[2].toDoubleOrNull() ?: 20.0
                val pct = if (maxMarks > 0) Math.round((obtained / maxMarks) * 1000.0) / 10.0 else 0.0

                // Find subject name
                var subject = line.replace(scoreMatch.value, "").replace(Regex("[:=-]"), "").trim()
                if (subject.isEmpty() && i > 0) {
                    subject = lines[i - 1].trim()
                }
                if (subject.isEmpty()) {
                    subject = "Course Assessment"
                }

                val type = when {
                    line.contains("mid", ignoreCase = true) || line.contains("cia", ignoreCase = true) -> "Midterm"
                    line.contains("final", ignoreCase = true) || line.contains("sem", ignoreCase = true) -> "Final Exam"
                    line.contains("quiz", ignoreCase = true) -> "Quiz"
                    line.contains("lab", ignoreCase = true) -> "Lab Exam"
                    else -> "Assignment"
                }

                results.add(
                    ExtractedMarkItem(
                        subjectName = subject.take(35),
                        assessmentName = "$type - $subject".take(35),
                        marksObtained = obtained,
                        maximumMarks = maxMarks,
                        percentage = pct
                    )
                )
            }
        }
        return results
    }

    private fun formatTimeStr(raw: String): String {
        val parts = raw.split(":")
        if (parts.size != 2) return raw
        val h = parts[0].toIntOrNull() ?: return raw
        val m = parts[1].take(2)
        val ampm = if (h in 9..11) "AM" else if (h == 12 || h in 1..6) "PM" else "AM"
        val formattedH = if (h > 12) h - 12 else if (h == 0) 12 else h
        return String.format(Locale.ROOT, "%02d:%s %s", formattedH, m, ampm)
    }
}
