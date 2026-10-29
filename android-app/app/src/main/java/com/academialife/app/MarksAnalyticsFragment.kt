package com.academialife.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
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

    private lateinit var tvAverageMarks: TextView
    private lateinit var tvTotalAssessments: TextView
    private lateinit var tvTopSubject: TextView
    private lateinit var rvAssessments: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var adapter: MarksAdapter

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
}
