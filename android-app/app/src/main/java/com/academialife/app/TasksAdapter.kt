package com.academialife.app

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TasksAdapter(
    private var items: List<Task>,
    private val onStatusToggled: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbCompleted: CheckBox = view.findViewById(R.id.cbTaskCompleted)
        val tvTitle: TextView = view.findViewById(R.id.tvTaskTitle)
        val tvSubject: TextView = view.findViewById(R.id.tvTaskSubject)
        val tvPriorityReason: TextView = view.findViewById(R.id.tvPriorityReason)
        val tvPriorityBadge: TextView = view.findViewById(R.id.tvPriorityBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvSubject.text = "${item.subjectName.ifEmpty { item.taskType.uppercase() }} • Due: ${item.dueDate}"
        holder.tvPriorityReason.text = item.priorityReason
        holder.tvPriorityBadge.text = "${item.priorityScore.toInt()} pts"

        val isCompleted = item.status == "completed"
        holder.cbCompleted.isChecked = isCompleted

        if (isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.setTextColor(Color.parseColor("#94A3B8"))
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.setTextColor(Color.parseColor("#0F172A"))
        }

        holder.cbCompleted.setOnClickListener {
            onStatusToggled(item, holder.cbCompleted.isChecked)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Task>) {
        items = newItems
        notifyDataSetChanged()
    }
}
