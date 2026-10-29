package com.academialife.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var tvHeader: TextView
    private lateinit var rvResults: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var adapter: SearchAdapter

    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_search, container, false)

        etSearch = root.findViewById(R.id.etGlobalSearch)
        tvHeader = root.findViewById(R.id.tvResultsHeader)
        rvResults = root.findViewById(R.id.rvSearchResults)
        layoutEmpty = root.findViewById(R.id.layoutEmptySearch)

        adapter = SearchAdapter(emptyList()) { item ->
            val main = activity as? MainActivity
            when (item.category) {
                "Timetable" -> main?.navigateTo(TimetableFragment(), "Timetable Schedule")
                "Task" -> main?.navigateTo(TasksFragment(), "Tasks & Examinations")
                "Subject", "Attendance" -> main?.navigateTo(AttendanceFragment(), "Attendance Compliance")
                "Assessment" -> main?.navigateTo(MarksAnalyticsFragment(), "Marks & Analytics")
                "Document" -> main?.navigateTo(DocumentVaultFragment(), "Course Document Vault")
            }
        }

        rvResults.layoutManager = LinearLayoutManager(context)
        rvResults.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString() ?: ""
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(250) // debounce
                    performSearch(q)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Initial search for all items
        performSearch("")
        return root
    }

    private fun performSearch(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (query.isBlank()) {
                val allTasks = FirebaseRepository.getTasks()
                val topResults = allTasks.take(5).map {
                    SearchResultItem(
                        id = it.id,
                        category = "Task",
                        title = it.title,
                        subtitle = "${it.taskType.replaceFirstChar { c -> c.uppercase() }} • Due: ${it.dueDate}",
                        details = "${it.priorityScore.toInt()} pts"
                    )
                }
                tvHeader.text = "RECENT ITEMS"
                adapter.updateList(topResults)
                layoutEmpty.visibility = if (topResults.isEmpty()) View.VISIBLE else View.GONE
            } else {
                val results = FirebaseRepository.globalSearch(query)
                tvHeader.text = "RESULTS FOR \"$query\" (${results.size})"
                adapter.updateList(results)
                layoutEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}

