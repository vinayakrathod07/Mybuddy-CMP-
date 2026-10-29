package com.academialife.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class DocumentVaultFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvDocs: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var adapter: DocumentAdapter
    private var allDocs: List<VaultDocument> = emptyList()
    private var selectedCategory: String = "All"
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_document_vault, container, false)

        etSearch = root.findViewById(R.id.etSearchDocs)
        chipGroup = root.findViewById(R.id.chipGroupDocCategories)
        rvDocs = root.findViewById(R.id.rvDocuments)
        layoutEmpty = root.findViewById(R.id.layoutEmptyDocs)
        fabAdd = root.findViewById(R.id.fabAddDocument)

        adapter = DocumentAdapter(
            emptyList(),
            onItemClick = { doc ->
                if (doc.fileUrl.isNotEmpty()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.fileUrl))
                        startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Cannot open URL: ${doc.fileUrl}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle(doc.title)
                        .setMessage("Course: ${doc.subjectName}\nCategory: ${doc.category}\n\nNotes:\n${doc.notes.ifEmpty { "No additional notes." }}")
                        .setPositiveButton("Close", null)
                        .show()
                }
            },
            onDeleteClick = { doc ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Document")
                    .setMessage("Remove '${doc.title}' from vault?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            FirebaseRepository.deleteDocument(doc.id)
                            Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                            loadDocuments()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        rvDocs.layoutManager = LinearLayoutManager(context)
        rvDocs.adapter = adapter

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chipDocNotes -> "Lecture Notes"
                R.id.chipDocSyllabus -> "Syllabus"
                R.id.chipDocPapers -> "Past Papers"
                R.id.chipDocCheatsheets -> "Cheatsheets"
                else -> "All"
            }
            filterList()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                filterList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fabAdd.setOnClickListener {
            showAddDocumentDialog()
        }

        loadDocuments()
        return root
    }

    private fun loadDocuments() {
        viewLifecycleOwner.lifecycleScope.launch {
            allDocs = FirebaseRepository.getDocuments()
            filterList()
        }
    }

    private fun filterList() {
        var filtered = allDocs

        if (selectedCategory != "All") {
            filtered = filtered.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                        it.subjectName.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q)
            }
        }

        adapter.updateList(filtered)
        layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDocumentDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_document, null)
        val etTitle = view.findViewById<EditText>(R.id.etDocTitle)
        val etSub = view.findViewById<EditText>(R.id.etDocSubject)
        val spCategory = view.findViewById<Spinner>(R.id.spDocCategory)
        val etUrl = view.findViewById<EditText>(R.id.etDocUrl)
        val etNotes = view.findViewById<EditText>(R.id.etDocNotes)

        val categories = arrayOf("Lecture Notes", "Syllabus", "Past Papers", "Cheatsheets", "References")
        spCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Document to Vault")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                val sub = etSub.text.toString().trim()
                val cat = spCategory.selectedItem.toString()
                val url = etUrl.text.toString().trim()
                val notes = etNotes.text.toString().trim()

                if (title.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val doc = VaultDocument(
                            title = title,
                            subjectName = sub,
                            category = cat,
                            fileUrl = url,
                            notes = notes
                        )
                        FirebaseRepository.addDocument(doc)
                        Toast.makeText(context, "Document added to Vault!", Toast.LENGTH_SHORT).show()
                        loadDocuments()
                    }
                } else {
                    Toast.makeText(context, "Document title is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

