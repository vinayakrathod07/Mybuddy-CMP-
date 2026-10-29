package com.academialife.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DocumentAdapter(
    private var items: List<VaultDocument>,
    private val onItemClick: ((VaultDocument) -> Unit)? = null,
    private val onDeleteClick: ((VaultDocument) -> Unit)? = null
) : RecyclerView.Adapter<DocumentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvDocTitle)
        val tvCategory: TextView = view.findViewById(R.id.tvDocCategory)
        val tvSubject: TextView = view.findViewById(R.id.tvDocSubject)
        val tvDetails: TextView = view.findViewById(R.id.tvDocDetails)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteDoc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vault_document, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title
        holder.tvCategory.text = item.category
        holder.tvSubject.text = if (item.subjectName.isNotEmpty()) "• ${item.subjectName}" else ""
        holder.tvDetails.text = "${item.fileSizeKb} KB • ${item.createdAt.ifEmpty { "Recent" }}"

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<VaultDocument>) {
        items = newItems
        notifyDataSetChanged()
    }
}

