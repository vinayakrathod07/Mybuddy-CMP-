package com.academialife.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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

    private lateinit var tvAverage: TextView
    private lateinit var tvTotal: TextView
    private lateinit var rvAssessments: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var fabAddMarks: FloatingActionButton
    private lateinit var adapter: MarksAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_marks, container, false)
        tvAverage = root.findViewById(R.id.tvAverageMarks)
        tvTotal = root.findViewById(R.id.tvTotalAssessments)
        rvAssessments = root.findViewById(R.id.rvAssessments)
        tvEmpty = root.findViewById(R.id.tvEmptyMarks)
        fabAddMarks = root.findViewById(R.id.fabAddMarks)

        adapter = MarksAdapter(emptyList())
        rvAssessments.layoutManager = LinearLayoutManager(context)
        rvAssessments.adapter = adapter

        fabAddMarks.setOnClickListener {
            showAddMarksDialog()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadMarks()
    }

    private fun loadMarks() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val list = FirebaseRepository.getAssessments()
                tvTotal.text = "${list.size}"
                if (list.isNotEmpty()) {
                    val avg = kotlin.math.round((list.map { it.percentage }.average()) * 10.0) / 10.0
                    tvAverage.text = "$avg%"
                    tvEmpty.visibility = View.GONE
                    rvAssessments.visibility = View.VISIBLE
                    adapter.updateList(list)
                } else {
                    tvAverage.text = "N/A"
                    tvEmpty.visibility = View.VISIBLE
                    rvAssessments.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading marks: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddMarksDialog() {
        val ctx = requireContext()
        val layout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etSubject = EditText(ctx).apply { hint = "Subject Code (e.g. CS301)" }
        val etName = EditText(ctx).apply { hint = "Assessment Name (e.g. Mid-Term 1)" }

        val spType = Spinner(ctx)
        spType.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, listOf("Quiz", "Mid-Term", "End-Term", "Assignment", "Project"))

        val etObtained = EditText(ctx).apply { hint = "Marks Obtained (e.g. 18.5)" }
        val etMax = EditText(ctx).apply { hint = "Maximum Marks (e.g. 20.0)"; setText("20.0") }

        layout.addView(etSubject)
        layout.addView(etName)
        layout.addView(spType)
        layout.addView(etObtained)
        layout.addView(etMax)

        AlertDialog.Builder(ctx)
            .setTitle("Record Assessment Marks")
            .setView(layout)
            .setPositiveButton("Save Score") { _, _ ->
                val code = etSubject.text.toString().trim().uppercase()
                val name = etName.text.toString().trim()
                val type = spType.selectedItem.toString()
                val obtained = etObtained.text.toString().toDoubleOrNull() ?: 0.0
                val maxMarks = etMax.text.toString().toDoubleOrNull() ?: 20.0

                if (name.isEmpty() || code.isEmpty()) {
                    Toast.makeText(ctx, "Name and Subject required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val subjects = FirebaseRepository.getSubjects()
                    val matchedSub = subjects.find { it.code.equals(code, ignoreCase = true) }
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    FirebaseRepository.addAssessment(
                        Assessment(
                            subjectCode = code,
                            subjectName = matchedSub?.name ?: code,
                            assessmentName = name,
                            assessmentType = type,
                            marksObtained = obtained,
                            maximumMarks = maxMarks,
                            assessmentDate = dateStr
                        )
                    )
                    Toast.makeText(ctx, "Score saved to Firestore!", Toast.LENGTH_SHORT).show()
                    loadMarks()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
