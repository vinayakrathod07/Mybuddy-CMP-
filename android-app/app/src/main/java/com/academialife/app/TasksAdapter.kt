package com.academialife.app

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class TasksAdapter(
    private var items: List<Task>,
    private val onTaskStatusChanged: (Task, Boolean) -> Unit,
    private val onDeleteClick: ((Task) -> Unit)? = null
) : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbCompleted: CheckBox = view.findViewById(R.id.cbTaskCompleted)
        val tvTitle: TextView = view.findViewById(R.id.tvTaskTitle)
        val tvSubject: TextView = view.findViewById(R.id.tvTaskSubject)
        val tvReason: TextView = view.findViewById(R.id.tvPriorityReason)
        val tvScore: TextView = view.findViewById(R.id.tvPriorityScore)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isCompleted = item.status == "completed"

        holder.tvTitle.text = item.title
        holder.tvSubject.text = buildString {
            if (item.subjectName.isNotEmpty()) {
                append(item.subjectName)
                append(" • ")
            }
            append(item.taskType.replaceFirstChar { it.uppercase() })
            if (item.dueDate.isNotEmpty()) {
                append(" • Due: ")
                append(item.dueDate)
            }
        }
        holder.tvReason.text = item.priorityReason

        val context = holder.itemView.context
        if (isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            holder.tvScore.text = "Done"
            holder.tvScore.setBackgroundResource(R.drawable.bg_pill_safe)
            holder.tvScore.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.tvScore.text = "${item.priorityScore.toInt()} pts"

            when {
                item.priorityScore >= 70.0 -> {
                    holder.tvScore.setBackgroundResource(R.drawable.bg_pill_critical)
                    holder.tvScore.setTextColor(ContextCompat.getColor(context, R.color.status_critical))
                }
                item.priorityScore >= 40.0 -> {
                    holder.tvScore.setBackgroundResource(R.drawable.bg_pill_warning)
                    holder.tvScore.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                }
                else -> {
                    holder.tvScore.setBackgroundResource(R.drawable.bg_pill_neutral)
                    holder.tvScore.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
                }
            }
        }

        // Avoid triggering listener when binding
        holder.cbCompleted.setOnCheckedChangeListener(null)
        holder.cbCompleted.isChecked = isCompleted
        holder.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            onTaskStatusChanged(item, isChecked)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Task>) {
        items = newItems
        notifyDataSetChanged()
    }
}
