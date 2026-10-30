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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class TimetableFragment : Fragment() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var rvTimetable: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: TimetableAdapter

    private var allEntries: List<TimetableEntry> = emptyList()
    private var selectedDay: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_timetable, container, false)

        chipGroup = root.findViewById(R.id.chipGroupDays)
        rvTimetable = root.findViewById(R.id.rvTimetable)
        layoutEmpty = root.findViewById(R.id.layoutEmptyTimetable)
        fabAdd = root.findViewById(R.id.fabAddClass)

        adapter = TimetableAdapter(emptyList()) { itemToDelete ->
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Class")
                .setMessage("Remove '${itemToDelete.subjectName}' from ${itemToDelete.dayOfWeek} schedule?")
                .setPositiveButton("Delete") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        FirebaseRepository.deleteTimetableEntry(itemToDelete.id)
                        Toast.makeText(context, "Class removed", Toast.LENGTH_SHORT).show()
                        loadTimetable()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        rvTimetable.layoutManager = LinearLayoutManager(context)
        rvTimetable.adapter = adapter

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedDay = when (checkedIds.firstOrNull()) {
                R.id.chipMonday -> "Monday"
                R.id.chipTuesday -> "Tuesday"
                R.id.chipWednesday -> "Wednesday"
                R.id.chipThursday -> "Thursday"
                R.id.chipFriday -> "Friday"
                R.id.chipSaturday -> "Saturday"
                else -> "All"
            }
            filterList()
        }

        // Auto-select current day of week on launch
        val today = java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(java.util.Date())
        val initialChipId = when (today) {
            "Monday" -> R.id.chipMonday
            "Tuesday" -> R.id.chipTuesday
            "Wednesday" -> R.id.chipWednesday
            "Thursday" -> R.id.chipThursday
            "Friday" -> R.id.chipFriday
            "Saturday" -> R.id.chipSaturday
            else -> R.id.chipAll
        }
        chipGroup.check(initialChipId)

        fabAdd.setOnClickListener {
            showAddClassDialog()
        }

        loadTimetable()
        return root
    }

    private fun loadTimetable() {
        viewLifecycleOwner.lifecycleScope.launch {
            allEntries = FirebaseRepository.getTimetable()
            filterList()
        }
    }

    private fun filterList() {
        val filtered = if (selectedDay == "All") {
            allEntries
        } else {
            allEntries.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
        }

        adapter.updateList(filtered)
        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddClassDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_class, null)
        val etSub = view.findViewById<EditText>(R.id.etSubName)
        val etCode = view.findViewById<EditText>(R.id.etSubCode)
        val spDay = view.findViewById<Spinner>(R.id.spDay)
        val etStart = view.findViewById<EditText>(R.id.etStart)
        val etEnd = view.findViewById<EditText>(R.id.etEnd)
        val etRoom = view.findViewById<EditText>(R.id.etRoom)
        val etFaculty = view.findViewById<EditText>(R.id.etFaculty)

        val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, days)
        spDay.adapter = spinnerAdapter
        if (selectedDay != "All") {
            val idx = days.indexOf(selectedDay)
            if (idx >= 0) spDay.setSelection(idx)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Class Schedule")
            .setView(view)
            .setPositiveButton("Schedule") { _, _ ->
                val name = etSub.text.toString().trim()
                val code = etCode.text.toString().trim()
                val day = spDay.selectedItem.toString()
                val start = etStart.text.toString().trim().ifEmpty { "09:00" }
                val end = etEnd.text.toString().trim().ifEmpty { "10:30" }
                val room = etRoom.text.toString().trim().ifEmpty { "Hall 101" }
                val faculty = etFaculty.text.toString().trim()

                if (name.isNotEmpty()) {
                    // Collision check
                    val conflict = AcademicCalculators.checkTimetableConflict(allEntries, day, start, end)
                    if (conflict != null) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("⚠️ Timetable Collision Warning")
                            .setMessage("Conflict detected with existing class '${conflict.subjectName}' scheduled from ${conflict.startTime} to ${conflict.endTime} in ${conflict.classroom}.\n\nDo you want to proceed and save anyway?")
                            .setPositiveButton("Yes, Save") { _, _ ->
                                saveEntry(name, code, day, start, end, room, faculty)
                            }
                            .setNegativeButton("Adjust Time", null)
                            .show()
                    } else {
                        saveEntry(name, code, day, start, end, room, faculty)
                    }
                } else {
                    Toast.makeText(context, "Subject name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveEntry(name: String, code: String, day: String, start: String, end: String, room: String, faculty: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val entry = TimetableEntry(
                subjectName = name,
                subjectCode = code,
                dayOfWeek = day,
                startTime = start,
                endTime = end,
                classroom = room,
                facultyName = faculty
            )
            FirebaseRepository.addTimetableEntry(entry)
            Toast.makeText(context, "Class scheduled on $day", Toast.LENGTH_SHORT).show()
            loadTimetable()
        }
    }
}
