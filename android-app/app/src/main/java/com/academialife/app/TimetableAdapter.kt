package com.academialife.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TimetableAdapter(
    private var items: List<TimetableEntry>,
    private val onDeleteClick: ((TimetableEntry) -> Unit)? = null
) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorBar: View = view.findViewById(R.id.viewColorBar)
        val tvDayBadge: TextView = view.findViewById(R.id.tvDayBadge)
        val tvSubjectCode: TextView = view.findViewById(R.id.tvSubjectCode)
        val tvSubjectName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvClassroom: TextView = view.findViewById(R.id.tvClassroom)
        val tvFaculty: TextView = view.findViewById(R.id.tvFaculty)
        val tvTimeSlot: TextView = view.findViewById(R.id.tvTimeSlot)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteTimetable)
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
        holder.tvClassroom.text = if (item.classroom.isNotEmpty()) "Room ${item.classroom}" else "Hall 101"
        holder.tvFaculty.text = item.facultyName.ifEmpty { "Department Faculty" }
        holder.tvTimeSlot.text = "${item.startTime} - ${item.endTime}"

        try {
            holder.colorBar.setBackgroundColor(Color.parseColor(item.subjectColor))
        } catch (_: Exception) {
            holder.colorBar.setBackgroundColor(Color.parseColor("#8175E5"))
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<TimetableEntry>) {
        items = newItems
        notifyDataSetChanged()
    }
}
