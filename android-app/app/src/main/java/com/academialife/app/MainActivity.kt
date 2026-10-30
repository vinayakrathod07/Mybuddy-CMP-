package com.academialife.app

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var topAppBar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        topAppBar = findViewById(R.id.topAppBar)
        bottomNav = findViewById(R.id.bottom_navigation)

        // Smooth back navigation to Dashboard
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (current !is DashboardFragment) {
                    bottomNav.selectedItemId = R.id.navigation_dashboard
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Initialize Firebase Anonymous/Auth in background
        lifecycleScope.launch {
            FirebaseRepository.ensureAuthenticated()
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    navigateTo(SearchFragment(), "Search Hub", "Instant Academic Lookup")
                    true
                }
                R.id.action_notifications -> {
                    navigateTo(NotificationsFragment(), "Alerts & Notifications", "Smart Academic Warnings")
                    true
                }
                R.id.action_chat -> {
                    navigateTo(AIChatFragment(), "AI Tutor Assistant", "Voice & Text RAG Tutor")
                    true
                }
                R.id.action_profile -> {
                    navigateTo(ProfileFragment(), "Student Profile", "Settings & Credentials")
                    true
                }
                else -> false
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    navigateTo(DashboardFragment(), "AcademiaLife", "Student Academic Assistant")
                    true
                }
                R.id.navigation_timetable -> {
                    navigateTo(TimetableFragment(), "Timetable Schedule", "Weekly Lectures & Labs")
                    true
                }
                R.id.navigation_tasks -> {
                    navigateTo(TasksFragment(), "Tasks & Examinations", "Deadlines & Priority Scoring")
                    true
                }
                R.id.navigation_study -> {
                    navigateTo(StudyPlannerFragment(), "Study Planner & Timer", "Pomodoro Focus & Routines")
                    true
                }
                R.id.navigation_attendance -> {
                    navigateTo(AttendanceFragment(), "Attendance Compliance", "Safety Margins & Safe Misses")
                    true
                }
                else -> false
            }
        }

        // Default landing screen: Dashboard
        if (savedInstanceState == null) {
            navigateTo(DashboardFragment(), "AcademiaLife", "Student Academic Assistant")
        }
    }

    fun navigateTo(fragment: Fragment, title: String, subtitle: String = "") {
        topAppBar.title = title
        topAppBar.subtitle = subtitle
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
