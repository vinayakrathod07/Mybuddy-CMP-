package com.academialife.app

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class AttendanceFragment : Fragment() {

    private lateinit var tvOverallAttendance: TextView
    private lateinit var tvOverallStatus: TextView
    private lateinit var tvOverallCounts: TextView
    private lateinit var tvTargetGoalBadge: TextView
    private lateinit var pbOverall: ProgressBar
    private lateinit var rvSubjects: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAddSubject: FloatingActionButton
    private lateinit var btnScanAI: TextView

    private lateinit var adapter: AttendanceAdapter

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                }
                processAttendanceBitmap(bitmap)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not load selected photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processAttendanceBitmap(bitmap)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_attendance, container, false)

        tvOverallAttendance = root.findViewById(R.id.tvOverallAttendance)
        tvOverallStatus = root.findViewById(R.id.tvOverallStatus)
        tvOverallCounts = root.findViewById(R.id.tvOverallCounts)
        tvTargetGoalBadge = root.findViewById(R.id.tvTargetGoalBadge)
        pbOverall = root.findViewById(R.id.pbOverallAttendance)
        rvSubjects = root.findViewById(R.id.rvAttendanceSubjects)
        layoutEmpty = root.findViewById(R.id.layoutEmptyAttendance)
        fabAddSubject = root.findViewById(R.id.fabAddSubject)
        btnScanAI = root.findViewById(R.id.btnScanAttendanceAI)

        btnScanAI.setOnClickListener {
            val options = arrayOf("📸 Take Photo of Attendance Sheet", "🖼️ Choose from Gallery")
            AlertDialog.Builder(requireContext())
                .setTitle("AI Attendance Scanner")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> cameraLauncher.launch(null)
                        1 -> galleryLauncher.launch("image/*")
                    }
                }
                .show()
        }

        adapter = AttendanceAdapter(
            emptyList(),
            onRecordAttendance = { subjectId, subjectName, subjectCode, status ->
                viewLifecycleOwner.lifecycleScope.launch {
                    FirebaseRepository.recordAttendance(subjectId, subjectName, subjectCode, status)
                    Toast.makeText(context, "Logged $status for $subjectName", Toast.LENGTH_SHORT).show()
                    loadAttendanceData()
                }
            },
            onDeleteClick = { subjectId, subjectName ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Course")
                    .setMessage("Remove '$subjectName' and its attendance logs?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            FirebaseRepository.deleteSubject(subjectId)
                            Toast.makeText(context, "Course removed", Toast.LENGTH_SHORT).show()
                            loadAttendanceData()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        rvSubjects.layoutManager = LinearLayoutManager(context)
        rvSubjects.adapter = adapter

        fabAddSubject.setOnClickListener {
            showAddSubjectDialog()
        }

        loadAttendanceData()
        return root
    }

    private fun loadAttendanceData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = FirebaseRepository.getProfile()
            val subjects = FirebaseRepository.getSubjects()
            val records = FirebaseRepository.getAttendanceRecords()

            val summary = AcademicCalculators.computeAttendanceSummary(subjects, records, profile.minimumAttendanceTarget)

            tvOverallAttendance.text = "${summary.overallPercentage}%"
            tvOverallCounts.text = "${summary.totalPresent} / ${summary.totalConducted} classes attended"
            tvTargetGoalBadge.text = "Target: ${profile.minimumAttendanceTarget.toInt()}%"
            pbOverall.progress = summary.overallPercentage.toInt()

            val context = requireContext()
            if (summary.isWarning) {
                tvOverallStatus.text = "Warning (< ${profile.minimumAttendanceTarget.toInt()}%)"
                tvOverallStatus.setBackgroundResource(R.drawable.bg_pill_warning)
                tvOverallStatus.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                tvOverallAttendance.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                pbOverall.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_warning))
            } else {
                tvOverallStatus.text = "Compliant (Healthy)"
                tvOverallStatus.setBackgroundResource(R.drawable.bg_pill_safe)
                tvOverallStatus.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
                tvOverallAttendance.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
                pbOverall.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_safe))
            }

            adapter.updateList(summary.subjectSummaries)
            layoutEmpty.visibility = if (summary.subjectSummaries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddSubjectDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_subject, null)
        val etName = view.findViewById<EditText>(R.id.etSubjectName)
        val etCode = view.findViewById<EditText>(R.id.etSubjectCode)
        val etFaculty = view.findViewById<EditText>(R.id.etFaculty)
        val etTarget = view.findViewById<EditText>(R.id.etTargetPct)

        AlertDialog.Builder(requireContext())
            .setTitle("Register Course")
            .setView(view)
            .setPositiveButton("Register") { _, _ ->
                val name = etName.text.toString().trim()
                val code = etCode.text.toString().trim()
                val faculty = etFaculty.text.toString().trim()
                val target = etTarget.text.toString().toDoubleOrNull() ?: 75.0

                if (name.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        FirebaseRepository.addSubject(name, code, faculty, targetAttendance = target)
                        Toast.makeText(context, "Course '$name' registered", Toast.LENGTH_SHORT).show()
                        loadAttendanceData()
                    }
                } else {
                    Toast.makeText(context, "Course name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processAttendanceBitmap(bitmap: Bitmap) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("🤖 Dost AI Vision")
            .setMessage("Analyzing attendance records from photo...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        viewLifecycleOwner.lifecycleScope.launch {
            val results = AIVisionAnalyzer.analyzeAttendancePhoto(bitmap)
            progressDialog.dismiss()

            if (results.isNotEmpty()) {
                val preview = results.take(5).joinToString("\n") {
                    "• ${it.subjectName}: ${it.attended}/${it.total} (${it.percentage}%)"
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("✨ AI Found ${results.size} Subject Records")
                    .setMessage("Extracted attendance:\n\n$preview\n\nWould you like to register/update these courses?")
                    .setPositiveButton("Sync All") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            for (item in results) {
                                val subject = FirebaseRepository.addSubject(item.subjectName, item.subjectName.take(4).uppercase(), "Faculty", color = "#7952F2", targetAttendance = 75.0)
                                for (i in 0 until item.attended) {
                                    FirebaseRepository.recordAttendance(subject.id, item.subjectName, subject.code, "present")
                                }
                                for (i in 0 until (item.total - item.attended)) {
                                    FirebaseRepository.recordAttendance(subject.id, item.subjectName, subject.code, "absent")
                                }
                            }
                            Toast.makeText(context, "Successfully synced ${results.size} course records!", Toast.LENGTH_LONG).show()
                            loadAttendanceData()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(context, "Could not detect attendance records in the photo. Please check image clarity.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
