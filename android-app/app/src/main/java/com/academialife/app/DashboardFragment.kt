package com.academialife.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var tvWelcomeName: TextView
    private lateinit var tvAcademicInfo: TextView
    private lateinit var tvTodayDate: TextView
    private lateinit var tvBriefingSummary: TextView
    private lateinit var tvAttendancePct: TextView
    private lateinit var tvAttendanceStatusBadge: TextView
    private lateinit var tvMarksAverage: TextView
    private lateinit var tvTotalAssessments: TextView
    private lateinit var tvPendingTasksCount: TextView
    private lateinit var tvTasksAlert: TextView
    private lateinit var tvClassesTodayCount: TextView
    private lateinit var tvClassesNextSummary: TextView

    private lateinit var layoutQuickAttendanceList: LinearLayout
    private lateinit var tvEmptyAttendance: TextView

    private lateinit var layoutTodaysClasses: LinearLayout
    private lateinit var tvEmptySchedule: TextView

    private lateinit var layoutHighPriorityTasks: LinearLayout
    private lateinit var tvEmptyTasks: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvWelcomeName = root.findViewById(R.id.tvWelcomeName)
        tvAcademicInfo = root.findViewById(R.id.tvAcademicInfo)
        tvTodayDate = root.findViewById(R.id.tvTodayDate)
        tvBriefingSummary = root.findViewById(R.id.tvBriefingSummary)
        tvAttendancePct = root.findViewById(R.id.tvAttendancePct)
        tvAttendanceStatusBadge = root.findViewById(R.id.tvAttendanceStatusBadge)
        tvMarksAverage = root.findViewById(R.id.tvMarksAverage)
        tvTotalAssessments = root.findViewById(R.id.tvTotalAssessments)
        tvPendingTasksCount = root.findViewById(R.id.tvPendingTasksCount)
        tvTasksAlert = root.findViewById(R.id.tvTasksAlert)
        tvClassesTodayCount = root.findViewById(R.id.tvClassesTodayCount)
        tvClassesNextSummary = root.findViewById(R.id.tvClassesNextSummary)

        layoutQuickAttendanceList = root.findViewById(R.id.layoutQuickAttendanceList)
        tvEmptyAttendance = root.findViewById(R.id.tvEmptyAttendance)

        layoutTodaysClasses = root.findViewById(R.id.layoutTodaysClasses)
        tvEmptySchedule = root.findViewById(R.id.tvEmptySchedule)

        layoutHighPriorityTasks = root.findViewById(R.id.layoutHighPriorityTasks)
        tvEmptyTasks = root.findViewById(R.id.tvEmptyTasks)

        // Setup Quick Actions
        val mainAct = activity as? MainActivity

        root.findViewById<View>(R.id.btnQuickProfile)?.setOnClickListener {
            mainAct?.navigateTo(ProfileFragment(), "Student Profile & Settings")
        }

        root.findViewById<View>(R.id.cardAttendanceMetric)?.setOnClickListener {
            mainAct?.navigateTo(AttendanceFragment(), "Attendance Compliance")
        }
        root.findViewById<View>(R.id.btnViewAllAttendance)?.setOnClickListener {
            mainAct?.navigateTo(AttendanceFragment(), "Attendance Compliance")
        }

        root.findViewById<View>(R.id.cardMarksMetric)?.setOnClickListener {
            mainAct?.navigateTo(MarksAnalyticsFragment(), "Marks & Analytics")
        }
        root.findViewById<View>(R.id.btnQuickMarks)?.setOnClickListener {
            mainAct?.navigateTo(MarksAnalyticsFragment(), "Marks & Analytics")
        }

        root.findViewById<View>(R.id.cardTasksMetric)?.setOnClickListener {
            mainAct?.navigateTo(TasksFragment(), "Tasks & Examinations")
        }
        root.findViewById<View>(R.id.btnViewAllTasks)?.setOnClickListener {
            mainAct?.navigateTo(TasksFragment(), "Tasks & Examinations")
        }

        root.findViewById<View>(R.id.cardTimetableMetric)?.setOnClickListener {
            mainAct?.navigateTo(TimetableFragment(), "Timetable Schedule")
        }
        root.findViewById<View>(R.id.btnViewAllTimetable)?.setOnClickListener {
            mainAct?.navigateTo(TimetableFragment(), "Timetable Schedule")
        }

        root.findViewById<View>(R.id.btnQuickStudyTimer)?.setOnClickListener {
            mainAct?.navigateTo(StudyPlannerFragment(), "Study Planner & Timer")
        }

        root.findViewById<View>(R.id.btnQuickAIChat)?.setOnClickListener {
            mainAct?.navigateTo(AIChatFragment(), "AI Tutor Assistant")
        }

        root.findViewById<View>(R.id.btnQuickVault)?.setOnClickListener {
            mainAct?.navigateTo(DocumentVaultFragment(), "Course Document Vault")
        }

        root.findViewById<View>(R.id.btnQuickSearch)?.setOnClickListener {
            mainAct?.navigateTo(SearchFragment(), "Search Hub")
        }

        loadDashboardData()
        return root
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = FirebaseRepository.getProfile()
            val timetable = FirebaseRepository.getTimetable()
            val tasks = FirebaseRepository.getTasks()
            val subjects = FirebaseRepository.getSubjects()
            val records = FirebaseRepository.getAttendanceRecords()
            val assessments = FirebaseRepository.getAssessments()

            val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records, profile.minimumAttendanceTarget)

            // 1. Profile header
            tvWelcomeName.text = "Welcome, ${profile.fullName} 👋"
            tvAcademicInfo.text = "${profile.college} • ${profile.semester} • ${profile.academicYear}"

            val sdfDay = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
            tvTodayDate.text = sdfDay.format(Date())

            // 2. Metrics
            tvAttendancePct.text = "${attSummary.overallPercentage}%"
            if (attSummary.isWarning) {
                tvAttendanceStatusBadge.text = "Warning"
                tvAttendanceStatusBadge.setBackgroundResource(R.drawable.bg_pill_warning)
                tvAttendanceStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning))
                tvAttendancePct.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning))
            } else {
                tvAttendanceStatusBadge.text = "Safe"
                tvAttendanceStatusBadge.setBackgroundResource(R.drawable.bg_pill_safe)
                tvAttendanceStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_safe))
                tvAttendancePct.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_safe))
            }

            if (assessments.isNotEmpty()) {
                val avg = kotlin.math.round((assessments.map { it.percentage }.average()) * 10.0) / 10.0
                tvMarksAverage.text = "$avg%"
                tvTotalAssessments.text = "${assessments.size} assessment(s) evaluated"
            } else {
                tvMarksAverage.text = "--"
                tvTotalAssessments.text = "0 assessments logged"
            }

            val pendingTasks = tasks.filter { it.status != "completed" }
            tvPendingTasksCount.text = "${pendingTasks.size}"
            val overdue = pendingTasks.count { it.isOverdue || it.priorityReason.contains("OVERDUE") }
            if (overdue > 0) {
                tvTasksAlert.text = "$overdue overdue task(s)!"
                tvTasksAlert.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_critical))
            } else {
                tvTasksAlert.text = if (pendingTasks.isEmpty()) "All coursework clear" else "${pendingTasks.size} active deadline(s)"
                tvTasksAlert.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }

            // 3. Today's classes
            val todayDayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
            val todaysClasses = timetable.filter { it.dayOfWeek.equals(todayDayName, ignoreCase = true) }
            tvClassesTodayCount.text = "${todaysClasses.size}"
            tvClassesNextSummary.text = if (todaysClasses.isNotEmpty()) "Next: ${todaysClasses.first().subjectName}" else "No classes today"

            // 4. Daily AI Briefing Text
            val briefing = StringBuilder()
            briefing.append("Good day, ${profile.fullName}. ")
            if (todaysClasses.isNotEmpty()) {
                briefing.append("You have ${todaysClasses.size} scheduled class(es) today, starting with ${todaysClasses.first().subjectName} at ${todaysClasses.first().startTime}. ")
            } else {
                briefing.append("You have no timetable classes scheduled for today. ")
            }

            if (attSummary.isWarning) {
                briefing.append("Attention: Your overall attendance is below your ${profile.minimumAttendanceTarget.toInt()}% target. ")
            }

            if (pendingTasks.isNotEmpty()) {
                val highest = pendingTasks.maxByOrNull { it.priorityScore }
                briefing.append("Top priority: '${highest?.title}' with a priority score of ${highest?.priorityScore?.toInt()} points.")
            } else {
                briefing.append("Your coursework is currently complete!")
            }
            tvBriefingSummary.text = briefing.toString()

            // 5. Populate Quick Attendance Logger
            layoutQuickAttendanceList.removeAllViews()
            if (subjects.isEmpty()) {
                tvEmptyAttendance.visibility = View.VISIBLE
            } else {
                tvEmptyAttendance.visibility = View.GONE
                subjects.take(4).forEach { sub ->
                    val card = layoutInflater.inflate(R.layout.item_quick_attendance, layoutQuickAttendanceList, false)
                    card.findViewById<TextView>(R.id.tvQuickSubName).text = sub.name
                    card.findViewById<TextView>(R.id.tvQuickSubCode).text = sub.code

                    card.findViewById<View>(R.id.btnQuickPresent).setOnClickListener {
                        viewLifecycleOwner.lifecycleScope.launch {
                            FirebaseRepository.recordAttendance(sub.id, sub.name, sub.code, "present")
                            Toast.makeText(context, "Marked Present for ${sub.name}", Toast.LENGTH_SHORT).show()
                            loadDashboardData()
                        }
                    }

                    card.findViewById<View>(R.id.btnQuickAbsent).setOnClickListener {
                        viewLifecycleOwner.lifecycleScope.launch {
                            FirebaseRepository.recordAttendance(sub.id, sub.name, sub.code, "absent")
                            Toast.makeText(context, "Marked Absent for ${sub.name}", Toast.LENGTH_SHORT).show()
                            loadDashboardData()
                        }
                    }

                    layoutQuickAttendanceList.addView(card)
                }
            }

            // 6. Populate Today's Classes List
            layoutTodaysClasses.removeAllViews()
            if (todaysClasses.isEmpty()) {
                tvEmptySchedule.visibility = View.VISIBLE
            } else {
                tvEmptySchedule.visibility = View.GONE
                todaysClasses.forEach { cl ->
                    val item = layoutInflater.inflate(R.layout.item_quick_schedule, layoutTodaysClasses, false)
                    item.findViewById<TextView>(R.id.tvScheduleSubName).text = cl.subjectName
                    item.findViewById<TextView>(R.id.tvScheduleTime).text = "${cl.startTime} - ${cl.endTime}"
                    item.findViewById<TextView>(R.id.tvScheduleRoom).text = "Room ${cl.classroom}"
                    layoutTodaysClasses.addView(item)
                }
            }

            // 7. Populate Top Priority Tasks
            layoutHighPriorityTasks.removeAllViews()
            if (pendingTasks.isEmpty()) {
                tvEmptyTasks.visibility = View.VISIBLE
            } else {
                tvEmptyTasks.visibility = View.GONE
                pendingTasks.sortedByDescending { it.priorityScore }.take(3).forEach { t ->
                    val item = layoutInflater.inflate(R.layout.item_quick_task, layoutHighPriorityTasks, false)
                    item.findViewById<TextView>(R.id.tvTaskTitle).text = t.title
                    item.findViewById<TextView>(R.id.tvTaskDue).text = "Due: ${t.dueDate}"
                    val tvScore = item.findViewById<TextView>(R.id.tvTaskScore)
                    tvScore.text = "${t.priorityScore.toInt()} pts"
                    tvScore.setBackgroundResource(if (t.priorityScore >= 70.0) R.drawable.bg_pill_critical else R.drawable.bg_pill_warning)
                    layoutHighPriorityTasks.addView(item)
                }
            }
        }
    }
}
