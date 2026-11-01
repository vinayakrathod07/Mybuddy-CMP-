package com.academialife.app

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object RoadmapDialogHelper {

    fun showRoadmapDialog(
        context: Context,
        scope: CoroutineScope,
        initialGoal: String = "Software Engineering & Tech Placements"
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_academic_roadmap, null)
        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = view.findViewById<TextView>(R.id.tvRoadmapTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvRoadmapSubtitle)
        val tvPhase = view.findViewById<TextView>(R.id.tvRoadmapPhase)
        val tvMilestones = view.findViewById<TextView>(R.id.tvRoadmapMilestones)
        val tvWhatToDo = view.findViewById<TextView>(R.id.tvRoadmapWhatToDo)
        val tvWhatToAvoid = view.findViewById<TextView>(R.id.tvRoadmapWhatToAvoid)
        val tvNextSteps = view.findViewById<TextView>(R.id.tvRoadmapNextSteps)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseRoadmap)
        val btnDone = view.findViewById<TextView>(R.id.btnDoneRoadmap)

        val chipTech = view.findViewById<TextView>(R.id.chipGoalTech)
        val chipCore = view.findViewById<TextView>(R.id.chipGoalCore)
        val chipHigher = view.findViewById<TextView>(R.id.chipGoalHigherStudies)
        val chipCgpa = view.findViewById<TextView>(R.id.chipGoalCgpa)

        val chips = listOf(
            Pair(chipTech, "Software Engineering & Tech Placements"),
            Pair(chipCore, "Core Engineering & Research"),
            Pair(chipHigher, "Higher Studies / GATE / GRE"),
            Pair(chipCgpa, "Academic Excellence & High CGPA")
        )

        fun loadData(goal: String) {
            scope.launch {
                val roadmap = StudentAIEngine.generatePersonalizedRoadmap(goal)
                withContext(Dispatchers.Main) {
                    tvTitle.text = "${roadmap.studentName}'s Roadmap"
                    tvSubtitle.text = "${roadmap.courseAndSem} • ${roadmap.targetGoal}"
                    tvPhase.text = "🎯 ${roadmap.currentPhaseTitle}"

                    tvMilestones.text = roadmap.milestones.joinToString("\n\n") { m ->
                        "📍 Month ${m.monthNumber}: ${m.title}\n" +
                        "• Focus: ${m.focus}\n" +
                        "• Deliverables: ${m.keyDeliverables.joinToString(", ")}"
                    }

                    tvWhatToDo.text = roadmap.whatToDo.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n\n")
                    tvWhatToAvoid.text = roadmap.whatToAvoid.joinToString("\n\n")
                    tvNextSteps.text = roadmap.immediateNextSteps.joinToString("\n")
                }
            }
        }

        fun updateChips(selectedGoal: String) {
            for ((chip, g) in chips) {
                if (g == selectedGoal) {
                    chip.setBackgroundResource(R.drawable.bg_neumorphic_btn_primary_idle)
                    chip.setTextColor(context.resources.getColor(R.color.white, null))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_neumorphic_chip_idle)
                    chip.setTextColor(context.resources.getColor(R.color.text_primary, null))
                }
            }
            loadData(selectedGoal)
        }

        for ((chip, g) in chips) {
            chip.setOnClickListener {
                updateChips(g)
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnDone.setOnClickListener { dialog.dismiss() }

        updateChips(initialGoal)
        dialog.show()
    }
}

