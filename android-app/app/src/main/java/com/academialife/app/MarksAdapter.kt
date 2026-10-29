package com.academialife.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MarksAdapter(
    private var items: List<Assessment>,
    private val onDeleteClick: ((Assessment) -> Unit)? = null
) : RecyclerView.Adapter<MarksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvAssessmentName)
        val tvSub: TextView = view.findViewById(R.id.tvAssessmentSubject)
        val tvDate: TextView = view.findViewById(R.id.tvAssessmentDate)
        val tvScore: TextView = view.findViewById(R.id.tvMarksScore)
        val tvPct: TextView = view.findViewById(R.id.tvMarksPercentage)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteAssessment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_assessment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvName.text = item.assessmentName
        holder.tvSub.text = "${item.subjectName} • ${item.assessmentType}"
        holder.tvDate.text = if (item.assessmentDate.isNotEmpty()) "Evaluated: ${item.assessmentDate}" else "Recent evaluation"
        holder.tvScore.text = "${item.marksObtained} / ${item.maximumMarks}"
        holder.tvPct.text = "${item.percentage}%"

        when {
            item.percentage >= 75.0 -> {
                holder.tvPct.setBackgroundResource(R.drawable.bg_pill_safe)
                holder.tvPct.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
            }
            item.percentage >= 50.0 -> {
                holder.tvPct.setBackgroundResource(R.drawable.bg_pill_warning)
                holder.tvPct.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
            }
            else -> {
                holder.tvPct.setBackgroundResource(R.drawable.bg_pill_critical)
                holder.tvPct.setTextColor(ContextCompat.getColor(context, R.color.status_critical))
            }
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Assessment>) {
        items = newItems
        notifyDataSetChanged()
    }
}
