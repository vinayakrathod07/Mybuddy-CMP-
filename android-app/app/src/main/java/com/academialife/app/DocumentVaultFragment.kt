package com.academialife.app

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
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

class DocumentVaultFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvDocs: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnScanDocAI: TextView

    private lateinit var adapter: DocumentAdapter
    private var allDocs: List<VaultDocument> = emptyList()
    private var selectedCategory: String = "All"
    private var searchQuery: String = ""

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
                processDocumentBitmap(bitmap)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not load selected document image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processDocumentBitmap(bitmap)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_document_vault, container, false)

        etSearch = root.findViewById(R.id.etSearchDocs)
        chipGroup = root.findViewById(R.id.chipGroupDocCategories)
        rvDocs = root.findViewById(R.id.rvDocuments)
        layoutEmpty = root.findViewById(R.id.layoutEmptyDocs)
        fabAdd = root.findViewById(R.id.fabAddDocument)
        btnScanDocAI = root.findViewById(R.id.btnScanDocAI)

        btnScanDocAI.setOnClickListener {
            val options = arrayOf("📸 Take Photo of Notes/Page", "🖼️ Choose from Gallery / Files")
            AlertDialog.Builder(requireContext())
                .setTitle("AI Document Scanner")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> cameraLauncher.launch(null)
                        1 -> galleryLauncher.launch("image/*")
                    }
                }
                .show()
        }

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

    private fun processDocumentBitmap(bitmap: Bitmap) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("🤖 Dost AI Vision")
            .setMessage("Scanning document, recognizing text, and extracting key summary...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        viewLifecycleOwner.lifecycleScope.launch {
            val extracted = AIVisionAnalyzer.analyzeDocumentPhoto(bitmap)
            progressDialog.dismiss()

            if (extracted.rawText.isNotBlank()) {
                val preview = "Title: ${extracted.title}\n" +
                        "Subject: ${extracted.subjectName}\n" +
                        "Category: ${extracted.category}\n\n" +
                        "${extracted.notes}"

                AlertDialog.Builder(requireContext())
                    .setTitle("✨ AI Extracted Document")
                    .setMessage(preview)
                    .setPositiveButton("Save to Vault (+30 XP)") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val doc = VaultDocument(
                                title = extracted.title,
                                subjectName = extracted.subjectName,
                                category = extracted.category,
                                notes = extracted.notes
                            )
                            FirebaseRepository.addDocument(doc)
                            FirebaseRepository.addXP(30, "AI scanned course document")
                            Toast.makeText(context, "Document saved to Vault! (+30 XP)", Toast.LENGTH_SHORT).show()
                            loadDocuments()
                        }
                    }
                    .setNeutralButton("Edit Details") { _, _ ->
                        // Pre-fill manual add dialog
                        val view = layoutInflater.inflate(R.layout.dialog_add_document, null)
                        val etTitle = view.findViewById<EditText>(R.id.etDocTitle)
                        val etSub = view.findViewById<EditText>(R.id.etDocSubject)
                        val spCategory = view.findViewById<Spinner>(R.id.spDocCategory)
                        val etNotes = view.findViewById<EditText>(R.id.etDocNotes)

                        val categories = arrayOf("Lecture Notes", "Syllabus", "Past Papers", "Cheatsheets", "References")
                        spCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)

                        etTitle.setText(extracted.title)
                        etSub.setText(extracted.subjectName)
                        val catIdx = categories.indexOf(extracted.category)
                        if (catIdx >= 0) spCategory.setSelection(catIdx)
                        etNotes.setText(extracted.notes)

                        AlertDialog.Builder(requireContext())
                            .setTitle("Review & Save Document")
                            .setView(view)
                            .setPositiveButton("Save") { _, _ ->
                                viewLifecycleOwner.lifecycleScope.launch {
                                    val doc = VaultDocument(
                                        title = etTitle.text.toString().trim().ifEmpty { extracted.title },
                                        subjectName = etSub.text.toString().trim().ifEmpty { extracted.subjectName },
                                        category = spCategory.selectedItem.toString(),
                                        notes = etNotes.text.toString().trim()
                                    )
                                    FirebaseRepository.addDocument(doc)
                                    Toast.makeText(context, "Document saved to Vault!", Toast.LENGTH_SHORT).show()
                                    loadDocuments()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(context, "No clear text detected. Please check lighting and angle.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

