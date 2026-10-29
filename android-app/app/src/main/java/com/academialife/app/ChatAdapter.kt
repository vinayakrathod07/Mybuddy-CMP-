package com.academialife.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private var items: List<ChatMessage>,
    private val onSpeakText: (String) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutUser: View = view.findViewById(R.id.layoutUserMessage)
        val tvUser: TextView = view.findViewById(R.id.tvUserContent)

        val layoutAssistant: View = view.findViewById(R.id.layoutAssistantMessage)
        val tvAssistant: TextView = view.findViewById(R.id.tvAssistantContent)
        val tvSources: TextView = view.findViewById(R.id.tvSources)
        val btnTts: View = view.findViewById(R.id.btnTtsRead)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = items[position]

        if (msg.role == "user") {
            holder.layoutUser.visibility = View.VISIBLE
            holder.layoutAssistant.visibility = View.GONE
            holder.tvUser.text = msg.content
        } else {
            holder.layoutUser.visibility = View.GONE
            holder.layoutAssistant.visibility = View.VISIBLE
            holder.tvAssistant.text = msg.content

            if (msg.sources.isNotEmpty()) {
                holder.tvSources.visibility = View.VISIBLE
                holder.tvSources.text = msg.sources
            } else {
                holder.tvSources.visibility = View.GONE
            }

            holder.btnTts.setOnClickListener {
                onSpeakText(msg.content)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ChatMessage>) {
        items = newItems
        notifyDataSetChanged()
    }
}
