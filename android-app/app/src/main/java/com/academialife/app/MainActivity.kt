package com.academialife.app

import android.os.Bundle
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

        // Initialize Firebase Anonymous/Auth in background
        lifecycleScope.launch {
            FirebaseRepository.ensureAuthenticated()
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    navigateTo(SearchFragment(), "Search Hub")
                    true
                }
                R.id.action_notifications -> {
                    navigateTo(NotificationsFragment(), "Alerts & Notifications")
                    true
                }
                R.id.action_chat -> {
                    navigateTo(AIChatFragment(), "AI Tutor Assistant")
                    true
                }
                R.id.action_profile -> {
                    navigateTo(ProfileFragment(), "Student Profile & Settings")
                    true
                }
                else -> false
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    navigateTo(DashboardFragment(), "AcademiaLife")
                    true
                }
                R.id.navigation_timetable -> {
                    navigateTo(TimetableFragment(), "Timetable Schedule")
                    true
                }
                R.id.navigation_tasks -> {
                    navigateTo(TasksFragment(), "Tasks & Examinations")
                    true
                }
                R.id.navigation_study -> {
                    navigateTo(StudyPlannerFragment(), "Study Planner & Timer")
                    true
                }
                R.id.navigation_attendance -> {
                    navigateTo(AttendanceFragment(), "Attendance Compliance")
                    true
                }
                else -> false
            }
        }

        // Default landing screen: Dashboard
        if (savedInstanceState == null) {
            navigateTo(DashboardFragment(), "AcademiaLife")
        }
    }

    fun navigateTo(fragment: Fragment, title: String) {
        topAppBar.title = title
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
