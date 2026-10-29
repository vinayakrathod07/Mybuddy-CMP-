package com.academialife.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class NotificationsAdapter(
    private var items: List<NotificationItem>
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivNotifIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvNotifMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.message

        when (item.type) {
            "attendance_warning" -> {
                holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_critical))
            }
            "deadline", "exam" -> {
                holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_warning))
            }
            else -> {
                holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.brand_primary))
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<NotificationItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

