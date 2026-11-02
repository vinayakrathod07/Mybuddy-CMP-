package com.academialife.app

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var etCollege: EditText
    private lateinit var etCourse: EditText
    private lateinit var etSemester: EditText
    private lateinit var etSection: EditText
    private lateinit var etGoal: EditText
    private lateinit var etAttendance: EditText
    private lateinit var btnSubmit: TextView
    private lateinit var btnSkip: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        etCollege = findViewById(R.id.etOnboardingCollege)
        etCourse = findViewById(R.id.etOnboardingCourse)
        etSemester = findViewById(R.id.etOnboardingSemester)
        etSection = findViewById(R.id.etOnboardingSection)
        etGoal = findViewById(R.id.etOnboardingGoal)
        etAttendance = findViewById(R.id.etOnboardingAttendanceTarget)
        btnSubmit = findViewById(R.id.btnOnboardingSubmit)
        btnSkip = findViewById(R.id.btnOnboardingSkip)

        btnSubmit.setOnClickListener {
            saveOnboardingData()
        }

        btnSkip.setOnClickListener {
            completeAndLaunch()
        }
    }

    private fun saveOnboardingData() {
        val college = etCollege.text.toString().trim()
        val course = etCourse.text.toString().trim()
        val semester = etSemester.text.toString().trim()
        val section = etSection.text.toString().trim()
        val goal = etGoal.text.toString().trim()
        val target = etAttendance.text.toString().toDoubleOrNull() ?: 75.0

        lifecycleScope.launch {
            val current = FirebaseRepository.getProfile()
            val updated = current.copy(
                college = college,
                course = course,
                semester = semester,
                section = section,
                careerGoals = goal,
                minimumAttendanceTarget = target,
                isOnboardingCompleted = true
            )
            FirebaseRepository.saveProfile(updated)
            Toast.makeText(this@OnboardingActivity, "Academic profile initialized!", Toast.LENGTH_SHORT).show()
            completeAndLaunch()
        }
    }

    private fun completeAndLaunch() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

