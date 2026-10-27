package com.academialife.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceAdapter(
    private var items: List<SubjectAttendanceSummary>,
    private val onQuickAction: (SubjectAttendanceSummary, String) -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCode: TextView = view.findViewById(R.id.tvSubjectCode)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusBadge)
        val tvName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvPct: TextView = view.findViewById(R.id.tvAttendancePct)
        val tvFraction: TextView = view.findViewById(R.id.tvAttendanceFraction)
        val pbAttendance: ProgressBar = view.findViewById(R.id.pbAttendance)
        val tvExplanation: TextView = view.findViewById(R.id.tvExplanation)
        val btnPresent: Button = view.findViewById(R.id.btnQuickPresent)
        val btnAbsent: Button = view.findViewById(R.id.btnQuickAbsent)
        val btnLeave: Button = view.findViewById(R.id.btnQuickLeave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_subject, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvCode.text = item.subjectCode
        holder.tvName.text = item.subjectName
        holder.tvPct.text = "${item.percentage}%"
        holder.tvFraction.text = "(${item.presentCount}/${item.totalClasses} attended)"
        holder.pbAttendance.progress = item.percentage.toInt()
        holder.tvExplanation.text = item.explanation
        holder.tvStatus.text = item.status.uppercase()

        when (item.status) {
            "safe" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#10B981"))
                holder.tvStatus.setBackgroundColor(Color.parseColor("#ECFDF5"))
            }
            "warning" -> {
                holder.tvStatus.setTextColor(Color.parseColor("#F59E0B"))
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFFBEB"))
            }
            else -> {
                holder.tvStatus.setTextColor(Color.parseColor("#EF4444"))
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FEF2F2"))
            }
        }

        holder.btnPresent.setOnClickListener { onQuickAction(item, "present") }
        holder.btnAbsent.setOnClickListener { onQuickAction(item, "absent") }
        holder.btnLeave.setOnClickListener { onQuickAction(item, "leave") }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<SubjectAttendanceSummary>) {
        items = newItems
        notifyDataSetChanged()
    }
}
