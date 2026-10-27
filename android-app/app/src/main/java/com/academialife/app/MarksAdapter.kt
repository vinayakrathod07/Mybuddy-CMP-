package com.academialife.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MarksAdapter(private var items: List<Assessment>) :
    RecyclerView.Adapter<MarksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvAssessmentName)
        val tvSubject: TextView = view.findViewById(R.id.tvAssessmentSubject)
        val tvDate: TextView = view.findViewById(R.id.tvAssessmentDate)
        val tvScore: TextView = view.findViewById(R.id.tvMarksScore)
        val tvPercentage: TextView = view.findViewById(R.id.tvPercentageBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_assessment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.assessmentName
        holder.tvSubject.text = "${item.subjectName.ifEmpty { item.subjectCode }} • ${item.assessmentType}"
        holder.tvDate.text = item.assessmentDate
        holder.tvScore.text = "${item.marksObtained} / ${item.maximumMarks}"
        holder.tvPercentage.text = "${item.percentage}%"

        if (item.percentage >= 75.0) {
            holder.tvPercentage.setTextColor(Color.parseColor("#10B981"))
            holder.tvPercentage.setBackgroundColor(Color.parseColor("#ECFDF5"))
        } else if (item.percentage >= 60.0) {
            holder.tvPercentage.setTextColor(Color.parseColor("#F59E0B"))
            holder.tvPercentage.setBackgroundColor(Color.parseColor("#FFFBEB"))
        } else {
            holder.tvPercentage.setTextColor(Color.parseColor("#EF4444"))
            holder.tvPercentage.setBackgroundColor(Color.parseColor("#FEF2F2"))
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Assessment>) {
        items = newItems
        notifyDataSetChanged()
    }
}
