package com.academialife.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StudentAIEngine {

    private var cachedInsight: AIProactiveInsight? = null

    suspend fun runProactiveAnalysis(): AIProactiveInsight = withContext(Dispatchers.IO) {
        try {
            val timetable = FirebaseRepository.getTimetable()
            val tasks = FirebaseRepository.getTasks()
            val subjects = FirebaseRepository.getSubjects()
            val records = FirebaseRepository.getAttendanceRecords()
            val profile = FirebaseRepository.getProfile()
            val wellness = FirebaseRepository.getTodayWellness()
            val gamification = FirebaseRepository.getGamification()

            val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records, profile.minimumAttendanceTarget)

            val todayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
            val todaysClasses = timetable.filter { it.dayOfWeek.equals(todayName, ignoreCase = true) }
            val pendingTasks = tasks.filter { it.status != "completed" }
            val urgentTasks = pendingTasks.filter { it.priorityScore >= 70.0 || it.isOverdue }

            val messageParts = mutableListOf<String>()
            var suggestedAction = "Explore Schedule"
            var actionTarget = "timetable"
            var warningSubject = ""

            // 1. Attendance Check
            val criticalSub = attSummary.subjectSummaries.firstOrNull { it.status == "critical" || it.status == "warning" }
            if (criticalSub != null) {
                warningSubject = criticalSub.subjectName
                messageParts.add("⚠️ Attendance Alert: '${criticalSub.subjectName}' is at ${criticalSub.percentage}%. Attend next ${criticalSub.classesNeeded} class(es) to stay compliant.")
                suggestedAction = "View Attendance"
                actionTarget = "attendance"
            }

            // 2. Urgent Deadlines
            if (urgentTasks.isNotEmpty()) {
                val topTask = urgentTasks.maxByOrNull { it.priorityScore }
                messageParts.add("⚡ High Priority: '${topTask?.title}' due ${topTask?.dueDate} (${topTask?.priorityScore?.toInt()} pts).")
                if (criticalSub == null) {
                    suggestedAction = "Open Planner"
                    actionTarget = "tasks"
                }
            }

            // 3. Today's Lectures
            if (todaysClasses.isNotEmpty()) {
                messageParts.add("🗓️ You have ${todaysClasses.size} lecture(s) today, starting with ${todaysClasses.first().subjectName} at ${todaysClasses.first().startTime}.")
            } else {
                messageParts.add("🎉 No classes scheduled today! Great opportunity for study blocks or resting.")
            }

            // 4. Hydration & Habits
            if (wellness.waterGlasses < 4) {
                messageParts.add("💧 Tip: You've had ${wellness.waterGlasses}/8 glasses of water today. Drink 1 glass to boost focus (+15 XP)!")
            }

            val finalTitle = if (urgentTasks.isNotEmpty() || criticalSub != null) {
                "⚡ Dost's Action Required Strategy"
            } else {
                "✨ Dost's Optimal Routine Strategy"
            }

            val combinedMessage = messageParts.joinToString(" ")

            val insight = AIProactiveInsight(
                title = finalTitle,
                message = combinedMessage,
                urgentCount = urgentTasks.size,
                attendanceWarningSubject = warningSubject,
                suggestedAction = suggestedAction,
                actionTarget = actionTarget,
                timestamp = System.currentTimeMillis()
            )

            cachedInsight = insight
            insight
        } catch (_: Exception) {
            AIProactiveInsight()
        }
    }

    suspend fun generatePersonalizedRoadmap(chosenGoal: String? = null): StudentRoadmap = withContext(Dispatchers.IO) {
        val profile = FirebaseRepository.getProfile()
        val subjects = FirebaseRepository.getSubjects()
        val records = FirebaseRepository.getAttendanceRecords()
        val attSummary = AcademicCalculators.computeAttendanceSummary(subjects, records, profile.minimumAttendanceTarget)

        val semText = profile.semester.ifEmpty { "Semester 5" }
        val course = profile.course.ifEmpty { "B.Tech Computer Science" }
        val name = profile.fullName.ifEmpty { "Dost" }

        val goal = chosenGoal ?: "Software Engineering & Tech Placement"

        val isCS = course.contains("Computer", ignoreCase = true) || course.contains("CSE", ignoreCase = true) || course.contains("IT", ignoreCase = true)
        val semNum = Regex("\\d+").find(semText)?.value?.toIntOrNull() ?: 5

        val currentPhase = when {
            semNum <= 2 -> "Phase 1: Academic Foundations & Problem Solving"
            semNum <= 4 -> "Phase 2: Core Concepts, Data Structures & Mini-Projects"
            semNum <= 6 -> "Phase 3: Advanced Systems, Portfolios & Internship Drive"
            else -> "Phase 4: Placement Mastery, Capstone & Career Launch"
        }

        val milestones = mutableListOf<RoadmapMilestone>()
        if (isCS) {
            when {
                semNum <= 2 -> {
                    milestones.add(RoadmapMilestone(1, "Month 1: Language Mastery", "Learn C/C++ or Java/Python basics with 50 practice problems", listOf("Loops, arrays, recursion", "Basic Git & GitHub setup", "Submit all lab exercises early")))
                    milestones.add(RoadmapMilestone(2, "Month 2: Linear Data Structures", "Arrays, Linked Lists, Stacks & Queues", listOf("Solve 30 Easy problems on LeetCode", "Build a CLI Student Record Manager", "Keep attendance > 80%")))
                    milestones.add(RoadmapMilestone(3, "Month 3: Math & Discrete Logic", "Boolean algebra, graphs basics, complexity analysis", listOf("Big-O notation mastery", "Midterm exam preparation sprints", "Form a 2-person study group")))
                    milestones.add(RoadmapMilestone(4, "Month 4: First Mini Project & Finals", "Build a small console or GUI project and ace semester exams", listOf("Publish project on GitHub with README", "Solve past 3 years university exam papers", "CGPA target: 8.5+")))
                }
                semNum <= 4 -> {
                    milestones.add(RoadmapMilestone(1, "Month 1: Advanced DSA & OOP", "Trees, Heaps, Graphs & Object Oriented Design", listOf("Binary trees & BFS/DFS traversal", "OOP principles (Encapsulation, Polymorphism)", "Daily 1 LeetCode problem")))
                    milestones.add(RoadmapMilestone(2, "Month 2: Database Systems & SQL", "Relational DB design, Normalization (1NF-BCNF) & Queries", listOf("Design an E-Commerce or Library schema", "Master JOINs, Indexes & Transactions", "Connect DB to backend code")))
                    milestones.add(RoadmapMilestone(3, "Month 3: Operating Systems & Networks", "Processes, Threads, Deadlocks & OSI 7-Layer model", listOf("Simulate Banker's algorithm", "Learn HTTP, TCP vs UDP, DNS", "Prepare for semester midterms")))
                    milestones.add(RoadmapMilestone(4, "Month 4: Full-Stack / App Mini Project", "Complete a production-ready web or mobile app", listOf("Add authentication & database", "Host on Vercel/Render or deploy APK", "Final semester exam revision")))
                }
                semNum <= 6 -> {
                    milestones.add(RoadmapMilestone(1, "Month 1: DSA Speed & LeetCode Grind", "Medium-level Arrays, Dynamic Programming & Graphs", listOf("Target: 75+ Medium LeetCode problems", "Time management: 25 mins per problem", "Create a crisp 1-page tech resume")))
                    milestones.add(RoadmapMilestone(2, "Month 2: High-Impact Portfolio Project", "Build a full-stack SaaS or AI-powered student utility", listOf("Modern tech stack (Kotlin/React/Node/Firebase)", "Include CI/CD, live demo URL & docs", "Apply to 5 off-campus internships/week")))
                    milestones.add(RoadmapMilestone(3, "Month 3: System Design & Aptitude", "High-level system design + Quantitative Aptitude & Core CS", listOf("Caching, Load balancing, DB Sharding basics", "Solve Speed Math & Logical Reasoning", "Mock interview with friends or senior")))
                    milestones.add(RoadmapMilestone(4, "Month 4: Internship Interviews & Finals", "Convert internship calls and secure 8.0+ semester SGPA", listOf("Revise CS fundamentals (OS, DBMS, CN)", "Solve previous company interview rounds", "Final exams focus sprints")))
                }
                else -> {
                    milestones.add(RoadmapMilestone(1, "Month 1: Placement Season Blitz", "Company-specific coding tests and online assessments (OA)", listOf("Solve Top 100 Liked interview questions", "Aptitude & Verbal daily 30-min practice", "Finalize professional LinkedIn & GitHub")))
                    milestones.add(RoadmapMilestone(2, "Month 2: Technical & HR Rounds", "Live coding, data structures, and behavioral STAR stories", listOf("Mock interviews with peers", "Deep dive into your project architecture", "Clear on-campus placement rounds")))
                    milestones.add(RoadmapMilestone(3, "Month 3: Capstone Major Project", "Execute comprehensive senior design project with faculty", listOf("Complete system implementation & testing", "Draft project report & IEEE research paper", "Ensure attendance requirements are met")))
                    milestones.add(RoadmapMilestone(4, "Month 4: Offer Negotiation & Transition", "Review job offers or graduate school admits & celebrate!", listOf("Upskill on production workflows (Docker, K8s)", "Complete final degree clearances", "Graduation readiness with high CGPA!")))
                }
            }
        } else {
            milestones.add(RoadmapMilestone(1, "Month 1: Core Fundamentals & Theory", "Master departmental core engineering principles and lab rigor", listOf("Complete all practical experiments", "Deep dive into core textbook references", "Maintain 85%+ attendance buffer")))
            milestones.add(RoadmapMilestone(2, "Month 2: Industry Software & Simulation Tools", "Learn key industry software (CAD, MATLAB, SolidWorks, PLC, etc.)", listOf("Complete 2 simulation case studies", "Obtain online industry certification", "Form study group for numerical subjects")))
            milestones.add(RoadmapMilestone(3, "Month 3: Technical Paper & Aptitude", "Aptitude preparation + GATE / Core industry recruitment practice", listOf("Practice 5 years GATE/Technical papers", "Revise thermodynamics/mechanics/circuits", "Midterm exams revision")))
            milestones.add(RoadmapMilestone(4, "Month 4: Industry Capstone & Final SGPA", "Fabrication / simulation project submission and final exams", listOf("Final project demonstration", "Ace semester theory examinations", "Aim for top 10% department rank")))
        }

        val whatToDo = mutableListOf<String>()
        whatToDo.add("Maintain an 80%+ Attendance Margin: Never gamble near the 75% edge. High attendance gives you faculty goodwill, better internal marks, and zero exam hall stress.")
        whatToDo.add("Daily 50-Minute Deep Focus Sprint: Consistency always wins. 50 minutes of distraction-free study every single day accomplishes more than an 8-hour panic cram the night before exams.")
        whatToDo.add("Active Recall Over Passive Reading: Test yourself using Dost AI quizzes or self-questioning. If you can explain it without looking at the notes, you truly know it.")
        whatToDo.add("Build and Ship 2 Portfolio Projects: Theory gets you past exams, but tangible, deployed projects get you internships, research grants, and high-paying jobs.")
        whatToDo.add("Track Every Task & Deadline in MyBuddy: Clear your mental cache. Let MyBuddy track upcoming submissions so your brain can focus on learning.")

        val whatToAvoid = mutableListOf<String>()
        whatToAvoid.add("🚫 Avoid Tutorial Hell: Never spend weeks watching tutorial videos without writing code or building from scratch. Active struggle is where actual learning happens.")
        whatToAvoid.add("🚫 Avoid Submitting Assignments Late: Late submissions forfeit 20% to 50% of easy internal evaluation marks that directly damage your final CGPA.")
        whatToAvoid.add("🚫 Avoid Skipping Classes Randomly: 'Bunking' feels harmless until you need to attend 14 consecutive classes just to reach 75%. Keep your buffer safe.")
        whatToAvoid.add("🚫 Avoid Rote Memorization of Solutions: Interviewers and modern university questions test core edge cases and first principles. Understand the 'why' before the 'how'.")
        whatToAvoid.add("🚫 Avoid Sacrificing Sleep for Cramming: Sleeping less than 6 hours degrades cognitive memory consolidation by up to 40%. Rest well to perform well.")

        val nextSteps = mutableListOf<String>()
        if (attSummary.overallPercentage < 75.0) {
            nextSteps.add("⚠️ Urgent: Attend all scheduled classes this week to bring your attendance back to ${profile.minimumAttendanceTarget}%.")
        } else {
            nextSteps.add("✅ Great job: Your current attendance is safe (${attSummary.overallPercentage}%). Keep up the discipline!")
        }
        nextSteps.add("🎯 Start Month 1 Milestone: '${milestones.firstOrNull()?.title}' starting today.")
        nextSteps.add("⚡ Open the Study tab and complete one 25-min Pomodoro session for your hardest subject.")
        nextSteps.add("📝 Review pending tasks in the Planner and clear any overdue assignments.")

        StudentRoadmap(
            studentName = name,
            courseAndSem = "$course • $semText",
            targetGoal = goal,
            currentPhaseTitle = currentPhase,
            milestones = milestones,
            whatToDo = whatToDo,
            whatToAvoid = whatToAvoid,
            immediateNextSteps = nextSteps,
            motivationalQuote = "Success is not an accident. It is hard work, perseverance, learning, and most of all, loving what you are doing! 🚀"
        )
    }

    fun getLatestInsight(): AIProactiveInsight {
        return cachedInsight ?: AIProactiveInsight()
    }
}

