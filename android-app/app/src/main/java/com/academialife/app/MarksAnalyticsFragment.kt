package com.academialife.app

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarksAnalyticsFragment : Fragment() {

    private lateinit var tvAverageMarks: TextView
    private lateinit var tvTotalAssessments: TextView
    private lateinit var tvTopSubject: TextView
    private lateinit var rvAssessments: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnScanMarksAI: TextView
    private lateinit var cardGrowthPathway: LinearLayout

    private lateinit var adapter: MarksAdapter
    private var cachedAssessments: List<Assessment> = emptyList()

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
                processMarksBitmap(bitmap)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not load selected score card photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processMarksBitmap(bitmap)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_marks, container, false)

        tvAverageMarks = root.findViewById(R.id.tvAverageMarks)
        tvTotalAssessments = root.findViewById(R.id.tvTotalAssessments)
        tvTopSubject = root.findViewById(R.id.tvTopSubject)
        rvAssessments = root.findViewById(R.id.rvAssessments)
        layoutEmpty = root.findViewById(R.id.layoutEmptyMarks)
        fabAdd = root.findViewById(R.id.fabAddAssessment)
        btnScanMarksAI = root.findViewById(R.id.btnScanMarksAI)
        cardGrowthPathway = root.findViewById(R.id.cardGrowthPathway)

        btnScanMarksAI.setOnClickListener {
            val options = arrayOf("📸 Take Photo of Marks Slip / Score Card", "🖼️ Choose from Gallery")
            AlertDialog.Builder(requireContext())
                .setTitle("AI Marks & Grade Scanner")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> cameraLauncher.launch(null)
                        1 -> galleryLauncher.launch("image/*")
                    }
                }
                .show()
        }

        cardGrowthPathway.setOnClickListener {
            showGrowthPathwayDialog()
        }

        adapter = MarksAdapter(emptyList()) { item ->
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Assessment")
                .setMessage("Remove '${item.assessmentName}' from records?")
                .setPositiveButton("Delete") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        FirebaseRepository.deleteAssessment(item.id)
                        Toast.makeText(context, "Assessment deleted", Toast.LENGTH_SHORT).show()
                        loadMarksData()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        rvAssessments.layoutManager = LinearLayoutManager(context)
        rvAssessments.adapter = adapter

        fabAdd.setOnClickListener {
            showAddAssessmentDialog()
        }

        loadMarksData()
        return root
    }

    private fun loadMarksData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = FirebaseRepository.getAssessments()
            tvTotalAssessments.text = "${list.size}"

            if (list.isNotEmpty()) {
                val avg = kotlin.math.round((list.map { it.percentage }.average()) * 10.0) / 10.0
                tvAverageMarks.text = "$avg%"

                val top = list.maxByOrNull { it.percentage }
                tvTopSubject.text = "${top?.percentage?.toInt()}%"
            } else {
                tvAverageMarks.text = "--"
                tvTopSubject.text = "--"
            }

            cachedAssessments = list
            adapter.updateList(list)
            layoutEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddAssessmentDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_assessment, null)
        val etName = view.findViewById<EditText>(R.id.etAssessmentName)
        val etSub = view.findViewById<EditText>(R.id.etSubjectName)
        val spType = view.findViewById<Spinner>(R.id.spAssessmentType)
        val etObtained = view.findViewById<EditText>(R.id.etMarksObtained)
        val etMax = view.findViewById<EditText>(R.id.etMaxMarks)
        val etDate = view.findViewById<EditText>(R.id.etAssessmentDate)

        val types = arrayOf("Quiz", "Midterm", "Final Exam", "Assignment", "Lab Exam")
        spType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        etDate.setText(sdf.format(Date()))

        AlertDialog.Builder(requireContext())
            .setTitle("Record Assessment")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val sub = etSub.text.toString().trim()
                val type = spType.selectedItem.toString()
                val obtained = etObtained.text.toString().toDoubleOrNull() ?: 0.0
                val maxMarks = etMax.text.toString().toDoubleOrNull() ?: 20.0
                val date = etDate.text.toString().trim().ifEmpty { sdf.format(Date()) }

                if (name.isNotEmpty() && sub.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val ass = Assessment(
                            assessmentName = name,
                            subjectName = sub,
                            assessmentType = type,
                            marksObtained = obtained,
                            maximumMarks = maxMarks,
                            assessmentDate = date
                        )
                        FirebaseRepository.addAssessment(ass)
                        Toast.makeText(context, "Assessment recorded!", Toast.LENGTH_SHORT).show()
                        loadMarksData()
                    }
                } else {
                    Toast.makeText(context, "Assessment name and subject are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processMarksBitmap(bitmap: Bitmap) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("🤖 Dost AI Vision")
            .setMessage("Scanning score card / marks slip and extracting marks...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        viewLifecycleOwner.lifecycleScope.launch {
            val results = AIVisionAnalyzer.analyzeMarksSlip(bitmap)
            progressDialog.dismiss()

            if (results.isNotEmpty()) {
                val preview = results.take(6).joinToString("\n") {
                    "• ${it.subjectName}: ${it.marksObtained}/${it.maximumMarks} (${it.percentage}%)"
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("✨ AI Found ${results.size} Assessment Scores")
                    .setMessage("Extracted marks:\n\n$preview\n\nWould you like to save these assessment records?")
                    .setPositiveButton("Save All (+35 XP)") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            for (item in results) {
                                val ass = Assessment(
                                    assessmentName = item.assessmentName,
                                    subjectName = item.subjectName,
                                    assessmentType = "Midterm",
                                    marksObtained = item.marksObtained,
                                    maximumMarks = item.maximumMarks,
                                    percentage = item.percentage,
                                    assessmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                )
                                FirebaseRepository.addAssessment(ass)
                            }
                            FirebaseRepository.addXP(35, "AI scanned exam score card")
                            Toast.makeText(context, "Successfully saved ${results.size} assessment marks! (+35 XP)", Toast.LENGTH_LONG).show()
                            loadMarksData()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(context, "Could not detect clear marks or score figures. Please check photo lighting.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showGrowthPathwayDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_academic_pathway, null)
        val etCurrent = view.findViewById<EditText>(R.id.etCurrentCGPA)
        val etTarget = view.findViewById<EditText>(R.id.etTargetCGPA)
        val etRemaining = view.findViewById<EditText>(R.id.etRemainingSemesters)
        val btnCalc = view.findViewById<TextView>(R.id.btnCalculateSGPA)
        val tvReqTitle = view.findViewById<TextView>(R.id.tvRequiredSGPATitle)
        val tvReqNote = view.findViewById<TextView>(R.id.tvRequiredSGPANote)
        val tvWeakDiag = view.findViewById<TextView>(R.id.tvWeakSubjectDiagnostics)

        // Pre-fill current CGPA based on average marks if available
        if (cachedAssessments.isNotEmpty()) {
            val avgPct = cachedAssessments.map { it.percentage }.average()
            val estCgpa = Math.round((avgPct / 10.0) * 10.0) / 10.0
            etCurrent.setText(String.format(Locale.ROOT, "%.1f", estCgpa))
        }

        btnCalc.setOnClickListener {
            val current = etCurrent.text.toString().toDoubleOrNull() ?: 7.5
            val target = etTarget.text.toString().toDoubleOrNull() ?: 8.5
            val remaining = etRemaining.text.toString().toIntOrNull() ?: 4

            val totalSems = 8
            val completedSems = (totalSems - remaining).coerceAtLeast(1)

            // Formula: Required SGPA = (Target * Total - Current * Completed) / Remaining
            val reqSGPA = ((target * totalSems) - (current * completedSems)) / remaining.toDouble()
            val rounded = Math.round(reqSGPA * 100.0) / 100.0

            if (rounded > 10.0) {
                tvReqTitle.text = "Target CGPA $target is mathematically out of range"
                tvReqTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning))
                tvReqNote.text = "Even scoring a perfect 10.0 SGPA across all $remaining remaining semesters yields a max CGPA of ${Math.round(((current * completedSems + 10.0 * remaining) / totalSems) * 100.0) / 100.0}. Try setting a target around ${Math.round(((current * completedSems + 9.5 * remaining) / totalSems) * 10.0) / 10.0}!"
            } else if (rounded <= current) {
                tvReqTitle.text = "Required Average SGPA: $rounded ✅"
                tvReqTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_safe))
                tvReqNote.text = "You are already performing above target! Maintaining an SGPA of $rounded in each remaining semester will easily secure your goal."
            } else {
                tvReqTitle.text = "Required Average SGPA: $rounded 🎯"
                tvReqTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.electric_violet))
                tvReqNote.text = "To jump from $current to $target CGPA in $remaining semesters, maintain an average SGPA of $rounded. Prioritize 4-credit courses for maximum grade boost!"
            }
        }

        // Run Weak Subject Diagnostic asynchronously
        viewLifecycleOwner.lifecycleScope.launch {
            val subjects = FirebaseRepository.getSubjects()
            val records = FirebaseRepository.getAttendanceRecords()
            val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records)

            val subjectAssessments = cachedAssessments.groupBy { it.subjectName.lowercase() }
            val diagnostics = mutableListOf<String>()

            for (subj in subjects) {
                val attItem = attSummary.subjectSummaries.firstOrNull { it.subjectName.equals(subj.name, ignoreCase = true) }
                val marksList = subjectAssessments[subj.name.lowercase()] ?: emptyList()
                val avgMark = if (marksList.isNotEmpty()) marksList.map { it.percentage }.average() else 70.0
                val attPct = attItem?.percentage ?: 80.0

                if (avgMark < 65.0 || attPct < 75.0) {
                    val reasons = mutableListOf<String>()
                    if (avgMark < 65.0) reasons.add("Low Marks (${Math.round(avgMark)}%)")
                    if (attPct < 75.0) reasons.add("Attendance Shortage (${attPct}%)")

                    diagnostics.add(
                        "⚠️ ${subj.name}: ${reasons.joinToString(" + ")}\n" +
                                "  💡 Action: Schedule 2 Pomodoro focus blocks & attend next 3 lectures without missing."
                    )
                }
            }

            if (diagnostics.isNotEmpty()) {
                tvWeakDiag.text = diagnostics.joinToString("\n\n")
                tvWeakDiag.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            } else {
                tvWeakDiag.text = "🌟 Excellent standing! All your registered subjects have healthy marks (>= 65%) and compliant attendance (>= 75%). Keep up the consistency!"
                tvWeakDiag.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_safe))
            }
        }

        AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Close", null)
            .show()
    }
}
