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
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvAuthBadge: TextView

    private lateinit var etFullName: EditText
    private lateinit var etCollege: EditText
    private lateinit var etCourse: EditText
    private lateinit var etSemester: EditText
    private lateinit var etYear: EditText
    private lateinit var etAttendanceTarget: EditText
    private lateinit var etStudyHours: EditText

    private lateinit var btnSave: TextView
    private lateinit var btnAuthDialog: TextView
    private lateinit var btnSignOut: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        tvProfileName = root.findViewById(R.id.tvProfileName)
        tvProfileEmail = root.findViewById(R.id.tvProfileEmail)
        tvAuthBadge = root.findViewById(R.id.tvAuthBadge)

        etFullName = root.findViewById(R.id.etEditFullName)
        etCollege = root.findViewById(R.id.etEditCollege)
        etCourse = root.findViewById(R.id.etEditCourse)
        etSemester = root.findViewById(R.id.etEditSemester)
        etYear = root.findViewById(R.id.etEditYear)
        etAttendanceTarget = root.findViewById(R.id.etEditAttendanceTarget)
        etStudyHours = root.findViewById(R.id.etEditStudyHours)

        btnSave = root.findViewById(R.id.btnSaveProfile)
        btnAuthDialog = root.findViewById(R.id.btnAuthDialog)
        btnSignOut = root.findViewById(R.id.btnSignOut)

        btnSave.setOnClickListener {
            saveProfileData()
        }

        btnAuthDialog.setOnClickListener {
            showAuthDialog()
        }

        btnSignOut.setOnClickListener {
            FirebaseRepository.signOut()
            Toast.makeText(context, "Signed out of current account", Toast.LENGTH_SHORT).show()
            loadProfileData()
        }

        loadProfileData()
        return root
    }

    private fun loadProfileData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val p = FirebaseRepository.getProfile()
            tvProfileName.text = p.fullName
            tvProfileEmail.text = p.email

            if (FirebaseRepository.isUserLoggedIn()) {
                tvAuthBadge.text = "Firebase Authenticated • Sync Enabled"
            } else {
                tvAuthBadge.text = "Local Guest Student Mode"
            }

            etFullName.setText(p.fullName)
            etCollege.setText(p.college)
            etCourse.setText(p.course)
            etSemester.setText(p.semester)
            etYear.setText(p.academicYear)
            etAttendanceTarget.setText("${p.minimumAttendanceTarget}")
            etStudyHours.setText("${p.preferredStudyHoursPerDay}")
        }
    }

    private fun saveProfileData() {
        val name = etFullName.text.toString().trim()
        val college = etCollege.text.toString().trim()
        val course = etCourse.text.toString().trim()
        val semester = etSemester.text.toString().trim()
        val year = etYear.text.toString().trim()
        val target = etAttendanceTarget.text.toString().toDoubleOrNull() ?: 75.0
        val studyHours = etStudyHours.text.toString().toDoubleOrNull() ?: 4.0

        if (name.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                val p = StudentProfile(
                    userId = FirebaseRepository.getCurrentUserId(),
                    email = tvProfileEmail.text.toString(),
                    fullName = name,
                    college = college,
                    course = course,
                    semester = semester,
                    academicYear = year,
                    minimumAttendanceTarget = target,
                    preferredStudyHoursPerDay = studyHours
                )
                FirebaseRepository.saveProfile(p)
                Toast.makeText(context, "Profile and compliance targets updated!", Toast.LENGTH_SHORT).show()
                loadProfileData()
            }
        } else {
            Toast.makeText(context, "Full name cannot be blank", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAuthDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_auth, null)
        val tabSignIn = view.findViewById<TextView>(R.id.tabSignIn)
        val tabRegister = view.findViewById<TextView>(R.id.tabRegister)
        val tvError = view.findViewById<TextView>(R.id.tvAuthError)
        val etName = view.findViewById<EditText>(R.id.etAuthName)
        val etEmail = view.findViewById<EditText>(R.id.etAuthEmail)
        val etPass = view.findViewById<EditText>(R.id.etAuthPassword)
        val btnForgot = view.findViewById<TextView>(R.id.btnForgotPassword)
        val btnSubmit = view.findViewById<TextView>(R.id.btnAuthSubmit)
        val btnCancel = view.findViewById<TextView>(R.id.btnAuthCancel)

        var isRegisterMode = false

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        fun updateTabs() {
            tvError.visibility = View.GONE
            if (isRegisterMode) {
                tabRegister.setBackgroundResource(R.drawable.bg_neumorphic_btn_primary_idle)
                tabRegister.setTextColor(resources.getColor(R.color.white, null))
                tabSignIn.setBackgroundResource(android.R.color.transparent)
                tabSignIn.setTextColor(resources.getColor(R.color.text_secondary, null))
                etName.visibility = View.VISIBLE
                btnForgot.visibility = View.GONE
                btnSubmit.text = "Create Account"
            } else {
                tabSignIn.setBackgroundResource(R.drawable.bg_neumorphic_btn_primary_idle)
                tabSignIn.setTextColor(resources.getColor(R.color.white, null))
                tabRegister.setBackgroundResource(android.R.color.transparent)
                tabRegister.setTextColor(resources.getColor(R.color.text_secondary, null))
                etName.visibility = View.GONE
                btnForgot.visibility = View.VISIBLE
                btnSubmit.text = "Sign In"
            }
        }

        tabSignIn.setOnClickListener {
            isRegisterMode = false
            updateTabs()
        }

        tabRegister.setOnClickListener {
            isRegisterMode = true
            updateTabs()
        }

        btnForgot.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                tvError.text = "Enter your student email first to receive a password reset link."
                tvError.visibility = View.VISIBLE
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    val (ok, errorMsg) = FirebaseRepository.sendPasswordReset(email)
                    if (ok) {
                        Toast.makeText(context, "Password reset email sent to $email!", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        tvError.text = errorMsg ?: "Failed to send reset email"
                        tvError.visibility = View.VISIBLE
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()
            val name = etName.text.toString().trim().ifEmpty { "Student" }

            if (email.isEmpty() || pass.isEmpty()) {
                tvError.text = "Please enter both email and password."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (pass.length < 6) {
                tvError.text = "Password must be at least 6 characters."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                btnSubmit.isEnabled = false
                btnSubmit.alpha = 0.6f
                if (isRegisterMode) {
                    val (ok, errorMsg) = FirebaseRepository.registerEmail(email, pass, name)
                    if (ok) {
                        Toast.makeText(context, "Welcome to AcademiaLife, $name!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadProfileData()
                    } else {
                        tvError.text = errorMsg ?: "Registration failed."
                        tvError.visibility = View.VISIBLE
                    }
                } else {
                    val (ok, errorMsg) = FirebaseRepository.signInEmail(email, pass)
                    if (ok) {
                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadProfileData()
                    } else {
                        tvError.text = errorMsg ?: "Invalid email or password."
                        tvError.visibility = View.VISIBLE
                    }
                }
                btnSubmit.isEnabled = true
                btnSubmit.alpha = 1.0f
            }
        }

        dialog.show()
    }
}

