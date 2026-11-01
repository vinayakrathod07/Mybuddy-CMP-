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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
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
    private lateinit var cardAttendanceGuide: LinearLayout

    private lateinit var adapter: AttendanceAdapter
    private var cachedSubjectSummaries: List<SubjectAttendanceSummary> = emptyList()

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
        cardAttendanceGuide = root.findViewById(R.id.cardAttendanceGuide)

        cardAttendanceGuide.setOnClickListener {
            showAttendanceGuideDialog()
        }

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

            cachedSubjectSummaries = summary.subjectSummaries
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

    private fun showAttendanceGuideDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_attendance_guide, null)
        val spSubject = view.findViewById<Spinner>(R.id.spGuideSubject)
        val tvCurrentPct = view.findViewById<TextView>(R.id.tvGuideCurrentPct)
        val tvCount = view.findViewById<TextView>(R.id.tvGuideAttendanceCount)
        val tvBufferStatus = view.findViewById<TextView>(R.id.tvGuideBufferStatus)

        val btnMinus = view.findViewById<TextView>(R.id.btnSimulateMinusMiss)
        val btnPlus = view.findViewById<TextView>(R.id.btnSimulatePlusMiss)
        val tvMissCount = view.findViewById<TextView>(R.id.tvSimulateMissCount)
        val tvResultPct = view.findViewById<TextView>(R.id.tvSimulatedResultPct)
        val tvAdvice = view.findViewById<TextView>(R.id.tvSimulatedAdvice)

        val list = cachedSubjectSummaries
        if (list.isEmpty()) {
            Toast.makeText(context, "Please add or scan courses to use the simulator!", Toast.LENGTH_SHORT).show()
            return
        }

        val subjectNames = list.map { "${it.subjectName} (${it.percentage}%)" }
        spSubject.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, subjectNames)

        var selectedIndex = 0
        var missSimulated = 1

        fun updateSimulation() {
            if (selectedIndex !in list.indices) return
            val item = list[selectedIndex]
            tvCurrentPct.text = "${item.percentage}%"
            tvCount.text = "${item.presentCount} attended / ${item.totalClasses} total"

            val canMiss = if (item.percentage >= item.targetPercentage && item.totalClasses > 0) {
                kotlin.math.floor((100.0 * item.presentCount - item.targetPercentage * item.totalClasses) / item.targetPercentage).toInt().coerceAtLeast(0)
            } else 0

            if (canMiss > 0) {
                tvBufferStatus.text = "✅ Safe buffer: Can miss $canMiss class(es)"
                tvBufferStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_safe))
            } else {
                val needed = if (item.classesNeeded > 0) item.classesNeeded else 1
                tvBufferStatus.text = "⚠️ Shortage: Must attend $needed consecutive class(es)"
                tvBufferStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning))
            }

            tvMissCount.text = "$missSimulated"
            val simulatedTotal = item.totalClasses + missSimulated
            val simulatedAttended = item.presentCount
            val simulatedPct = if (simulatedTotal > 0) {
                Math.round((simulatedAttended.toDouble() / simulatedTotal) * 1000.0) / 10.0
            } else 0.0

            tvResultPct.text = "Projected Attendance: $simulatedPct%"
            if (simulatedPct >= 75.0) {
                tvResultPct.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_safe))
                tvAdvice.text = "✅ SAFE! If you miss $missSimulated class(es), you stay above the mandatory 75% threshold."
                tvAdvice.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_safe))
            } else {
                tvResultPct.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning))
                val shortfall = Math.round((75.0 - simulatedPct) * 10.0) / 10.0
                tvAdvice.text = "⚠️ SHORTAGE DANGER! Dropping below 75% by $shortfall%. Condonation or extra attendance will be needed."
                tvAdvice.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning))
            }
        }

        spSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedIndex = position
                updateSimulation()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnMinus.setOnClickListener {
            if (missSimulated > 1) {
                missSimulated--
                updateSimulation()
            }
        }

        btnPlus.setOnClickListener {
            if (missSimulated < 20) {
                missSimulated++
                updateSimulation()
            }
        }

        updateSimulation()

        AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Close", null)
            .show()
    }
}
