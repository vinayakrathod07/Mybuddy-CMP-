package com.academialife.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TimetableAdapter(private var items: List<TimetableEntry>) :
    RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorBar: View = view.findViewById(R.id.viewColorBar)
        val tvDayBadge: TextView = view.findViewById(R.id.tvDayBadge)
        val tvSubjectCode: TextView = view.findViewById(R.id.tvSubjectCode)
        val tvSubjectName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvClassDetails: TextView = view.findViewById(R.id.tvClassDetails)
        val tvClassTime: TextView = view.findViewById(R.id.tvClassTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timetable, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvDayBadge.text = item.dayOfWeek
        holder.tvSubjectCode.text = item.subjectCode.ifEmpty { "COURSE" }
        holder.tvSubjectName.text = item.subjectName.ifEmpty { item.subjectCode }
        holder.tvClassDetails.text = buildString {
            append(item.classroom.ifEmpty { "Lecture Hall" })
            if (item.facultyName.isNotEmpty()) {
                append(" • ")
                append(item.facultyName)
            }
        }
        holder.tvClassTime.text = "${item.startTime} - ${item.endTime}"

        try {
            holder.colorBar.setBackgroundColor(Color.parseColor(item.subjectColor))
        } catch (_: Exception) {
            holder.colorBar.setBackgroundColor(Color.parseColor("#4F46E5"))
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<TimetableEntry>) {
        items = newItems
        notifyDataSetChanged()
    }
}
