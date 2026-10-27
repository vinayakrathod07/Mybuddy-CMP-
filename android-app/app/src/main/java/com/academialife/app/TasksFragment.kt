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

class TasksFragment : Fragment() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var adapter: TasksAdapter
    private var tasksList = listOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_tasks, container, false)
        rvTasks = root.findViewById(R.id.rvTasks)
        tvEmpty = root.findViewById(R.id.tvEmptyTasks)
        fabAddTask = root.findViewById(R.id.fabAddTask)

        adapter = TasksAdapter(emptyList()) { task, isCompleted ->
            viewLifecycleOwner.lifecycleScope.launch {
                FirebaseRepository.updateTaskStatus(task.id, if (isCompleted) "completed" else "pending")
                loadTasks()
            }
        }
        rvTasks.layoutManager = LinearLayoutManager(context)
        rvTasks.adapter = adapter

        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTasks()
    }

    private fun loadTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                tasksList = FirebaseRepository.getTasks()
                if (tasksList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvTasks.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvTasks.visibility = View.VISIBLE
                    adapter.updateList(tasksList)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddTaskDialog() {
        val ctx = requireContext()
        val layout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etTitle = EditText(ctx).apply { hint = "Task Title (e.g. Lab Assignment 2)" }
        val etSubject = EditText(ctx).apply { hint = "Subject Code (e.g. CS301)" }

        val spType = Spinner(ctx)
        spType.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, listOf("assignment", "examination", "project", "revision"))

        val spPriority = Spinner(ctx)
        spPriority.adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, listOf("medium", "high", "urgent", "low"))

        val defaultDue = SimpleDateFormat("yyyy-MM-dd'T'18:00", Locale.getDefault()).format(Date(System.currentTimeMillis() + 86400000L * 2))
        val etDue = EditText(ctx).apply { hint = "Due Date (yyyy-MM-dd'T'HH:mm)"; setText(defaultDue) }

        layout.addView(etTitle)
        layout.addView(etSubject)
        layout.addView(spType)
        layout.addView(spPriority)
        layout.addView(etDue)

        AlertDialog.Builder(ctx)
            .setTitle("New Coursework / Exam")
            .setView(layout)
            .setPositiveButton("Save & Score") { _, _ ->
                val title = etTitle.text.toString().trim()
                val subjectCode = etSubject.text.toString().trim().uppercase()
                val taskType = spType.selectedItem.toString()
                val priority = spPriority.selectedItem.toString()
                val due = etDue.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(ctx, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val subjects = FirebaseRepository.getSubjects()
                    val matchedSub = subjects.find { it.code.equals(subjectCode, ignoreCase = true) }

                    val newTask = Task(
                        title = title,
                        subjectCode = subjectCode,
                        subjectName = matchedSub?.name ?: subjectCode,
                        taskType = taskType,
                        priority = priority,
                        dueDate = due,
                        estimatedEffortMinutes = 60
                    )
                    FirebaseRepository.addTask(newTask)
                    Toast.makeText(ctx, "Task created in Firestore!", Toast.LENGTH_SHORT).show()
                    loadTasks()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
