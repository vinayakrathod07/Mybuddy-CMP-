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
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
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

        // Initialize Firebase Auth, Deadline Guardian Worker & Active Student AI Engine
        DeadlineGuardianWorker.schedulePeriodicCheck(this)
        lifecycleScope.launch {
            FirebaseRepository.ensureAuthenticated()
            StudentAIEngine.runProactiveAnalysis()
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
                    navigateTo(AIChatFragment(), "Dost — AI Tutor", "Multilingual Academic Companion")
                    true
                }
                R.id.action_profile -> {
                    navigateTo(ProfileFragment(), "Student Profile", "Settings & Preferences")
                    true
                }
                else -> false
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    navigateTo(DashboardFragment(), "MyBuddy", "Your Student Life Companion")
                    true
                }
                R.id.navigation_tasks -> {
                    navigateTo(TasksFragment(), "Tasks & Examinations", "Deadlines & Priority Scoring")
                    true
                }
                R.id.navigation_dost -> {
                    navigateTo(AIChatFragment(), "Dost — AI Tutor", "Multilingual Academic Companion")
                    true
                }
                R.id.navigation_timetable -> {
                    navigateTo(TimetableFragment(), "Timetable Schedule", "Weekly Lectures & Labs")
                    true
                }
                R.id.navigation_lifestyle -> {
                    navigateTo(LifestyleFragment(), "Student Life Zone", "Budget, Wellness & Badges")
                    true
                }
                else -> false
            }
        }

        // Default landing screen: Dashboard
        if (savedInstanceState == null) {
            navigateTo(DashboardFragment(), "MyBuddy", "Your Academic Companion")
        }
    }

    fun navigateTo(fragment: Fragment, title: String, subtitle: String = "") {
        if (isNavigating) return
        isNavigating = true
        topAppBar.title = title
        topAppBar.subtitle = subtitle
        topAppBar.visibility = if (fragment is DashboardFragment) android.view.View.GONE else android.view.View.VISIBLE

        val targetId = when (fragment) {
            is DashboardFragment -> R.id.navigation_dashboard
            is TasksFragment -> R.id.navigation_tasks
            is AIChatFragment -> R.id.navigation_dost
            is TimetableFragment -> R.id.navigation_timetable
            is LifestyleFragment -> R.id.navigation_lifestyle
            else -> null
        }
        if (targetId != null && bottomNav.selectedItemId != targetId) {
            bottomNav.menu.findItem(targetId)?.isChecked = true
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        isNavigating = false
    }
}
