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
    private lateinit var btnScanAI: TextView
    private lateinit var btnClearAll: TextView
    private lateinit var adapter: TimetableAdapter

    private var allEntries: List<TimetableEntry> = emptyList()
    private var selectedDay: String = "All"

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
                processTimetableBitmap(bitmap)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not load selected photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processTimetableBitmap(bitmap)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_timetable, container, false)

        chipGroup = root.findViewById(R.id.chipGroupDays)
        rvTimetable = root.findViewById(R.id.rvTimetable)
        layoutEmpty = root.findViewById(R.id.layoutEmptyTimetable)
        fabAdd = root.findViewById(R.id.fabAddClass)
        btnScanAI = root.findViewById(R.id.btnScanTimetableAI)
        btnClearAll = root.findViewById(R.id.btnClearTimetable)

        btnClearAll.setOnClickListener {
            handleClearTimetable()
        }

        btnScanAI.setOnClickListener {
            val options = arrayOf("📸 Take Photo of Timetable", "🖼️ Choose from Gallery")
            AlertDialog.Builder(requireContext())
                .setTitle("AI Timetable Scanner")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> cameraLauncher.launch(null)
                        1 -> galleryLauncher.launch("image/*")
                    }
                }
                .show()
        }

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

    private fun handleClearTimetable() {
        if (allEntries.isEmpty()) {
            Toast.makeText(context, "Timetable is already empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDay != "All") {
            val dayEntries = allEntries.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
            val options = arrayOf(
                "Clear classes on $selectedDay only (${dayEntries.size} classes)",
                "Clear entire weekly timetable (${allEntries.size} classes)"
            )

            AlertDialog.Builder(requireContext())
                .setTitle("Clear Timetable")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> confirmAndClearDay(selectedDay)
                        1 -> confirmAndClearAll()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            confirmAndClearAll()
        }
    }

    private fun confirmAndClearAll() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Entire Timetable?")
            .setMessage("Are you sure you want to delete all ${allEntries.size} scheduled classes across the entire week? This cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val success = FirebaseRepository.clearAllTimetable()
                    if (success) {
                        Toast.makeText(context, "Weekly timetable cleared", Toast.LENGTH_SHORT).show()
                        loadTimetable()
                    } else {
                        Toast.makeText(context, "Failed to clear timetable", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmAndClearDay(day: String) {
        val dayCount = allEntries.count { it.dayOfWeek.equals(day, ignoreCase = true) }
        AlertDialog.Builder(requireContext())
            .setTitle("Clear $day Schedule?")
            .setMessage("Are you sure you want to delete all $dayCount classes on $day?")
            .setPositiveButton("Clear $day") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val success = FirebaseRepository.clearDayTimetable(day)
                    if (success) {
                        Toast.makeText(context, "$day schedule cleared", Toast.LENGTH_SHORT).show()
                        loadTimetable()
                    } else {
                        Toast.makeText(context, "Failed to clear $day schedule", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun processTimetableBitmap(bitmap: Bitmap) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("🤖 Dost AI Vision")
            .setMessage("Analyzing timetable photo and detecting class schedules...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        viewLifecycleOwner.lifecycleScope.launch {
            val detected = AIVisionAnalyzer.analyzeTimetablePhoto(bitmap)
            progressDialog.dismiss()

            if (detected.isNotEmpty()) {
                val preview = detected.take(5).joinToString("\n") {
                    "• ${it.dayOfWeek}: ${it.subjectName} (${it.startTime} - ${it.endTime}) [${it.classroom}]"
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("✨ AI Found ${detected.size} Class(es)")
                    .setMessage("Schedule preview:\n\n$preview\n\nImport all detected classes into your timetable?")
                    .setPositiveButton("Import All") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            for (entry in detected) {
                                FirebaseRepository.addTimetableEntry(entry)
                            }
                            Toast.makeText(context, "Successfully imported ${detected.size} classes!", Toast.LENGTH_LONG).show()
                            loadTimetable()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(context, "No clear timetable entries detected in photo. Please ensure good lighting.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
