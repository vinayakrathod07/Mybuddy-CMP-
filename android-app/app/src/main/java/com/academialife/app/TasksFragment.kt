package com.academialife.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TasksFragment : Fragment() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var chipGroup: ChipGroup
    private lateinit var tvProgressText: TextView
    private lateinit var tvCompletionPct: TextView
    private lateinit var pbProgress: ProgressBar

    private lateinit var adapter: TasksAdapter
    private var allTasks: List<Task> = emptyList()
    private var filterMode: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_tasks, container, false)

        rvTasks = root.findViewById(R.id.rvTasks)
        layoutEmpty = root.findViewById(R.id.layoutEmptyTasks)
        fabAddTask = root.findViewById(R.id.fabAddTask)
        chipGroup = root.findViewById(R.id.chipGroupTaskFilters)
        tvProgressText = root.findViewById(R.id.tvTaskProgressText)
        tvCompletionPct = root.findViewById(R.id.tvTaskCompletionPct)
        pbProgress = root.findViewById(R.id.pbTaskProgress)

        adapter = TasksAdapter(
            emptyList(),
            onTaskStatusChanged = { task, isCompleted ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val newStatus = if (isCompleted) "completed" else "pending"
                    FirebaseRepository.updateTaskStatus(task.id, newStatus)
                    loadTasks()
                }
            },
            onDeleteClick = { task ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete '${task.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            FirebaseRepository.deleteTask(task.id)
                            Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
                            loadTasks()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        rvTasks.layoutManager = LinearLayoutManager(context)
        rvTasks.adapter = adapter

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            filterMode = when (checkedIds.firstOrNull()) {
                R.id.chipTaskPending -> "Pending"
                R.id.chipTaskCompleted -> "Completed"
                R.id.chipTaskExams -> "Exams"
                R.id.chipTaskAssignments -> "Assignments"
                else -> "All"
            }
            filterList()
        }

        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        loadTasks()
        return root
    }

    private fun loadTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            allTasks = FirebaseRepository.getTasks()
            updateProgressHeader()
            filterList()
        }
    }

    private fun updateProgressHeader() {
        val total = allTasks.size
        val completed = allTasks.count { it.status == "completed" }
        val pct = if (total > 0) (completed * 100) / total else 0

        tvProgressText.text = "$completed of $total Tasks Completed"
        tvCompletionPct.text = "$pct% Done"
        pbProgress.progress = pct
    }

    private fun filterList() {
        val filtered = when (filterMode) {
            "Pending" -> allTasks.filter { it.status != "completed" }
            "Completed" -> allTasks.filter { it.status == "completed" }
            "Exams" -> allTasks.filter { it.taskType.equals("examination", ignoreCase = true) }
            "Assignments" -> allTasks.filter { it.taskType.equals("assignment", ignoreCase = true) }
            else -> allTasks
        }

        adapter.updateList(filtered)
        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddTaskDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val etTitle = view.findViewById<EditText>(R.id.etTaskTitle)
        val spType = view.findViewById<Spinner>(R.id.spTaskType)
        val spPriority = view.findViewById<Spinner>(R.id.spPriority)
        val etDueDate = view.findViewById<EditText>(R.id.etDueDate)
        val etEffort = view.findViewById<EditText>(R.id.etEffort)
        val etDesc = view.findViewById<EditText>(R.id.etDescription)

        val taskTypes = arrayOf("assignment", "examination", "project", "revision")
        spType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, taskTypes)

        val priorities = arrayOf("medium", "high", "urgent", "low")
        spPriority.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities)

        // Default due date to 3 days from now
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val defaultDue = sdf.format(Date(System.currentTimeMillis() + 3L * 24 * 3600 * 1000))
        etDueDate.setText(defaultDue)

        AlertDialog.Builder(requireContext())
            .setTitle("Register Coursework / Exam")
            .setView(view)
            .setPositiveButton("Save Task") { _, _ ->
                val title = etTitle.text.toString().trim()
                val type = spType.selectedItem.toString()
                val priority = spPriority.selectedItem.toString()
                val due = etDueDate.text.toString().trim().ifEmpty { defaultDue }
                val effort = etEffort.text.toString().toIntOrNull() ?: 60
                val desc = etDesc.text.toString().trim()

                if (title.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val task = Task(
                            title = title,
                            taskType = type,
                            priority = priority,
                            dueDate = due,
                            estimatedEffortMinutes = effort,
                            description = desc
                        )
                        FirebaseRepository.addTask(task)
                        Toast.makeText(context, "Task registered with priority score!", Toast.LENGTH_SHORT).show()
                        loadTasks()
                    }
                } else {
                    Toast.makeText(context, "Task title is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
