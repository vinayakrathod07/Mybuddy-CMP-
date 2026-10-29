package com.academialife.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(
    private var items: List<SearchResultItem>,
    private val onItemClick: ((SearchResultItem) -> Unit)? = null
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tvSearchCategory)
        val tvDetails: TextView = view.findViewById(R.id.tvSearchDetails)
        val tvTitle: TextView = view.findViewById(R.id.tvSearchTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSearchSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvCategory.text = item.category
        holder.tvDetails.text = item.details
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<SearchResultItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

