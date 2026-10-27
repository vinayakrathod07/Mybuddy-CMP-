package com.academialife.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class TimetableFragment : Fragment() {

    private lateinit var chipGroupDays: ChipGroup
    private lateinit var rvTimetable: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var fabAddClass: FloatingActionButton

    private lateinit var adapter: TimetableAdapter
    private var allEntries = listOf<TimetableEntry>()
    private var subjectsList = listOf<Subject>()
    private var selectedDay = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_timetable, container, false)
        chipGroupDays = root.findViewById(R.id.chipGroupDays)
        rvTimetable = root.findViewById(R.id.rvTimetable)
        tvEmpty = root.findViewById(R.id.tvEmptyTimetable)
        fabAddClass = root.findViewById(R.id.fabAddClass)

        adapter = TimetableAdapter(emptyList())
        rvTimetable.layoutManager = LinearLayoutManager(context)
        rvTimetable.adapter = adapter

        setupDayChips()

        fabAddClass.setOnClickListener {
            showAddClassDialog()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTimetable()
    }

    private fun setupDayChips() {
        chipGroupDays.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedDay = when {
                checkedIds.contains(R.id.chipMonday) -> "Monday"
                checkedIds.contains(R.id.chipTuesday) -> "Tuesday"
                checkedIds.contains(R.id.chipWednesday) -> "Wednesday"
                checkedIds.contains(R.id.chipThursday) -> "Thursday"
                checkedIds.contains(R.id.chipFriday) -> "Friday"
                checkedIds.contains(R.id.chipSaturday) -> "Saturday"
                else -> "All"
            }
            filterTimetable()
        }
    }

    private fun loadTimetable() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                subjectsList = FirebaseRepository.getSubjects()
                allEntries = FirebaseRepository.getTimetable()
                filterTimetable()
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading timetable: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterTimetable() {
        val filtered = if (selectedDay == "All") {
            allEntries
        } else {
            allEntries.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
        }

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvTimetable.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvTimetable.visibility = View.VISIBLE
            adapter.updateList(filtered)
        }
    }

    private fun showAddClassDialog() {
        val ctx = requireContext()
        val layout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etSubject = EditText(ctx).apply { hint = "Subject Name (e.g. Operating Systems)" }
        val etCode = EditText(ctx).apply { hint = "Subject Code (e.g. CS301)" }
        val etDay = EditText(ctx).apply {
            hint = "Day of Week"
            setText(if (selectedDay != "All") selectedDay else "Monday")
        }
        val etStart = EditText(ctx).apply { hint = "Start Time (09:00)"; setText("09:00") }
        val etEnd = EditText(ctx).apply { hint = "End Time (10:30)"; setText("10:30") }
        val etRoom = EditText(ctx).apply { hint = "Classroom (Hall 101)"; setText("Hall 101") }

        layout.addView(etSubject)
        layout.addView(etCode)
        layout.addView(etDay)
        layout.addView(etStart)
        layout.addView(etEnd)
        layout.addView(etRoom)

        AlertDialog.Builder(ctx)
            .setTitle("Add Class Slot")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val subName = etSubject.text.toString().trim()
                val subCode = etCode.text.toString().trim()
                val day = etDay.text.toString().trim()
                val start = etStart.text.toString().trim()
                val end = etEnd.text.toString().trim()
                val room = etRoom.text.toString().trim()

                if (subName.isEmpty() || subCode.isEmpty()) {
                    Toast.makeText(ctx, "Subject Name and Code required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Check conflict
                val conflict = AcademicCalculators.checkTimetableConflict(allEntries, day, start, end)
                if (conflict != null) {
                    Toast.makeText(ctx, "Collision warning: overlaps with ${conflict.subjectName} (${conflict.startTime}-${conflict.endTime})", Toast.LENGTH_LONG).show()
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    var subject = subjectsList.find { it.code.equals(subCode, ignoreCase = true) }
                    if (subject == null) {
                        subject = FirebaseRepository.addSubject(subName, subCode, "")
                    }

                    FirebaseRepository.addTimetableEntry(
                        TimetableEntry(
                            subjectId = subject.id,
                            subjectName = subName,
                            subjectCode = subCode,
                            dayOfWeek = day,
                            startTime = start,
                            endTime = end,
                            classroom = room
                        )
                    )
                    Toast.makeText(ctx, "Class scheduled in Firestore!", Toast.LENGTH_SHORT).show()
                    loadTimetable()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
