package com.academialife.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AttendanceFragment : Fragment() {

    private lateinit var tvOverallPct: TextView
    private lateinit var tvOverallStatus: TextView
    private lateinit var rvSubjects: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: AttendanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_attendance, container, false)
        tvOverallPct = root.findViewById(R.id.tvOverallAttendance)
        tvOverallStatus = root.findViewById(R.id.tvOverallStatus)
        rvSubjects = root.findViewById(R.id.rvAttendanceSubjects)
        tvEmpty = root.findViewById(R.id.tvEmptyAttendance)

        adapter = AttendanceAdapter(emptyList()) { summary, status ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    FirebaseRepository.recordAttendance(
                        subjectId = summary.subjectId,
                        subjectName = summary.subjectName,
                        subjectCode = summary.subjectCode,
                        status = status
                    )
                    Toast.makeText(context, "Logged ${status.uppercase()} for ${summary.subjectName}!", Toast.LENGTH_SHORT).show()
                    loadAttendance()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to log attendance: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        rvSubjects.layoutManager = LinearLayoutManager(context)
        rvSubjects.adapter = adapter

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAttendance()
    }

    private fun loadAttendance() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val profile = FirebaseRepository.getProfile()
                val subjects = FirebaseRepository.getSubjects()
                val records = FirebaseRepository.getAttendanceRecords()

                val summary = AcademicCalculators.computeAttendanceSummary(subjects, records, profile.minimumAttendanceTarget)

                tvOverallPct.text = "${summary.overallPercentage}%"
                if (summary.isWarning) {
                    tvOverallStatus.text = "Shortage Alert"
                    tvOverallStatus.setTextColor(resources.getColor(R.color.status_warning, null))
                } else {
                    tvOverallStatus.text = "Safely Compliant"
                    tvOverallStatus.setTextColor(resources.getColor(R.color.status_safe, null))
                }

                if (summary.subjectSummaries.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvSubjects.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvSubjects.visibility = View.VISIBLE
                    adapter.updateList(summary.subjectSummaries)
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error loading attendance: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
