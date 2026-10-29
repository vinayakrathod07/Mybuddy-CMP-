package com.academialife.app

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class StudySessionAdapter(
    private var items: List<StudySession>,
    private val onToggleStatus: (StudySession) -> Unit,
    private val onDeleteClick: ((StudySession) -> Unit)? = null
) : RecyclerView.Adapter<StudySessionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stripe: View = view.findViewById(R.id.viewStudyStripe)
        val tvSub: TextView = view.findViewById(R.id.tvStudySubject)
        val tvDate: TextView = view.findViewById(R.id.tvStudyDate)
        val tvTitle: TextView = view.findViewById(R.id.tvStudyTitle)
        val tvReason: TextView = view.findViewById(R.id.tvStudyReason)
        val btnToggle: ImageView = view.findViewById(R.id.btnToggleSessionComplete)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteStudySession)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_study_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val isCompleted = item.status == "completed"

        holder.tvSub.text = item.subjectName.ifEmpty { "General Study" }
        holder.tvDate.text = "${item.sessionDate} • ${item.startTime} (${item.durationMinutes}m)"
        holder.tvTitle.text = item.title
        holder.tvReason.text = item.recommendationReason

        if (isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            holder.stripe.setBackgroundResource(R.drawable.bg_pill_safe)
            holder.btnToggle.setColorFilter(ContextCompat.getColor(context, R.color.status_safe))
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.stripe.setBackgroundResource(R.drawable.bg_pill_neutral)
            holder.btnToggle.setColorFilter(ContextCompat.getColor(context, R.color.text_muted))
        }

        holder.btnToggle.setOnClickListener {
            onToggleStatus(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<StudySession>) {
        items = newItems
        notifyDataSetChanged()
    }
}

