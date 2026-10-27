package com.academialife.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var topAppBar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        topAppBar = findViewById(R.id.topAppBar)
        bottomNav = findViewById(R.id.bottom_navigation)

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_chat -> {
                    replaceFragment(AIChatFragment(), "AI Assistant")
                    true
                }
                else -> false
            }
        }

        // Add Chat icon to top app bar
        topAppBar.inflateMenu(R.menu.top_app_bar_menu)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    replaceFragment(DashboardFragment(), "AcademiaLife")
                    true
                }
                R.id.navigation_timetable -> {
                    replaceFragment(TimetableFragment(), "Timetable Schedule")
                    true
                }
                R.id.navigation_tasks -> {
                    replaceFragment(TasksFragment(), "Tasks & Exams")
                    true
                }
                R.id.navigation_attendance -> {
                    replaceFragment(AttendanceFragment(), "Attendance Compliance")
                    true
                }
                R.id.navigation_marks -> {
                    replaceFragment(MarksAnalyticsFragment(), "Marks & Analytics")
                    true
                }
                else -> false
            }
        }

        // Default screen: Dashboard
        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment(), "AcademiaLife")
        }
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        topAppBar.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
