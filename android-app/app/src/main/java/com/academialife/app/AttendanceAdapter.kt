package com.academialife.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AttendanceAdapter(
    private var items: List<SubjectAttendanceSummary>,
    private val onRecordAttendance: (String, String, String, String) -> Unit,
    private val onDeleteClick: ((String, String) -> Unit)? = null
) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCode: TextView = view.findViewById(R.id.tvSubjectCode)
        val tvName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvPct: TextView = view.findViewById(R.id.tvSubjectPct)
        val tvCounts: TextView = view.findViewById(R.id.tvAttendanceCounts)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusBadge)
        val tvExp: TextView = view.findViewById(R.id.tvExplanation)
        val pbProgress: ProgressBar = view.findViewById(R.id.pbSubjectProgress)
        val btnPresent: TextView = view.findViewById(R.id.btnLogPresent)
        val btnAbsent: TextView = view.findViewById(R.id.btnLogAbsent)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteSubject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_subject, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvCode.text = item.subjectCode
        holder.tvName.text = item.subjectName
        holder.tvPct.text = "${item.percentage}%"
        holder.tvCounts.text = "${item.presentCount} / ${item.totalClasses} attended"
        holder.tvExp.text = item.explanation
        holder.pbProgress.progress = item.percentage.toInt()

        when (item.status) {
            "safe" -> {
                holder.tvStatus.text = "Safe"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_pill_safe)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
                holder.pbProgress.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_safe))
            }
            "warning" -> {
                holder.tvStatus.text = "Warning"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_pill_warning)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                holder.pbProgress.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_warning))
            }
            else -> {
                holder.tvStatus.text = "Critical"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_pill_critical)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_critical))
                holder.pbProgress.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_critical))
            }
        }

        holder.btnPresent.setOnClickListener {
            onRecordAttendance(item.subjectId, item.subjectName, item.subjectCode, "present")
        }

        holder.btnAbsent.setOnClickListener {
            onRecordAttendance(item.subjectId, item.subjectName, item.subjectCode, "absent")
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(item.subjectId, item.subjectName)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<SubjectAttendanceSummary>) {
        items = newItems
        notifyDataSetChanged()
    }
}
