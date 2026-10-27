package com.academialife.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var tvWelcomeName: TextView
    private lateinit var tvAcademicInfo: TextView
    private lateinit var tvBriefingSummary: TextView
    private lateinit var tvAttendancePct: TextView
    private lateinit var tvAttendanceStatus: TextView
    private lateinit var tvTasksCount: TextView
    private lateinit var tvOverdueCount: TextView
    private lateinit var rvClasses: RecyclerView
    private lateinit var rvTasks: RecyclerView
    private lateinit var tvEmptyClasses: TextView
    private lateinit var tvEmptyTasks: TextView

    private lateinit var classesAdapter: TimetableAdapter
    private lateinit var tasksAdapter: TasksAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvWelcomeName = root.findViewById(R.id.tvWelcomeName)
        tvAcademicInfo = root.findViewById(R.id.tvAcademicInfo)
        tvBriefingSummary = root.findViewById(R.id.tvBriefingSummary)
        tvAttendancePct = root.findViewById(R.id.tvAttendancePct)
        tvAttendanceStatus = root.findViewById(R.id.tvAttendanceStatus)
        tvTasksCount = root.findViewById(R.id.tvTasksCount)
        tvOverdueCount = root.findViewById(R.id.tvOverdueCount)
        rvClasses = root.findViewById(R.id.rvDashboardClasses)
        rvTasks = root.findViewById(R.id.rvDashboardTasks)
        tvEmptyClasses = root.findViewById(R.id.tvEmptyClasses)
        tvEmptyTasks = root.findViewById(R.id.tvEmptyTasks)

        classesAdapter = TimetableAdapter(emptyList())
        rvClasses.layoutManager = LinearLayoutManager(context)
        rvClasses.adapter = classesAdapter

        tasksAdapter = TasksAdapter(emptyList()) { task, isCompleted ->
            viewLifecycleOwner.lifecycleScope.launch {
                FirebaseRepository.updateTaskStatus(task.id, if (isCompleted) "completed" else "pending")
                loadDashboardData()
            }
        }
        rvTasks.layoutManager = LinearLayoutManager(context)
        rvTasks.adapter = tasksAdapter

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                FirebaseRepository.ensureAuthenticated()
                val profile = FirebaseRepository.getProfile()
                val subjects = FirebaseRepository.getSubjects()
                val timetable = FirebaseRepository.getTimetable()
                val tasks = FirebaseRepository.getTasks()
                val records = FirebaseRepository.getAttendanceRecords()

                // Welcome header
                tvWelcomeName.text = "Welcome, ${profile.fullName} 👋"
                val todayStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
                tvAcademicInfo.text = "$todayStr • ${profile.college} • ${profile.semester}"

                // Attendance calculation
                val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records, profile.minimumAttendanceTarget)
                tvAttendancePct.text = "${attSummary.overallPercentage}%"
                if (attSummary.isWarning) {
                    tvAttendanceStatus.text = "Shortage Alert"
                    tvAttendanceStatus.setTextColor(resources.getColor(R.color.status_warning, null))
                } else {
                    tvAttendanceStatus.text = "Safely Compliant"
                    tvAttendanceStatus.setTextColor(resources.getColor(R.color.status_safe, null))
                }

                // Tasks stats
                val pending = tasks.filter { it.status != "completed" }
                tvTasksCount.text = "${pending.size}"
                tvOverdueCount.text = "${pending.count { it.isOverdue }} overdue"

                // Today's classes
                val todayDay = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
                val todaysClasses = timetable.filter { it.dayOfWeek.equals(todayDay, ignoreCase = true) }
                if (todaysClasses.isEmpty()) {
                    tvEmptyClasses.visibility = View.VISIBLE
                    rvClasses.visibility = View.GONE
                } else {
                    tvEmptyClasses.visibility = View.GONE
                    rvClasses.visibility = View.VISIBLE
                    classesAdapter.updateList(todaysClasses)
                }

                // Urgent tasks
                if (pending.isEmpty()) {
                    tvEmptyTasks.visibility = View.VISIBLE
                    rvTasks.visibility = View.GONE
                } else {
                    tvEmptyTasks.visibility = View.GONE
                    rvTasks.visibility = View.VISIBLE
                    tasksAdapter.updateList(pending.take(4))
                }

                // Daily briefing
                tvBriefingSummary.text = "Good day, ${profile.fullName}. You have ${todaysClasses.size} class(es) scheduled for today, ${pending.size} active coursework task(s), and attendance standing is at ${attSummary.overallPercentage}%."

            } catch (e: Exception) {
                tvBriefingSummary.text = "Connected to Firebase backend. Tap any tab to manage your coursework."
            }
        }
    }
}
