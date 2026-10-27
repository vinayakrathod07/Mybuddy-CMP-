package com.academialife.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private var messages: List<ChatMessage>,
    private val onSpeakClicked: (String) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutUser: LinearLayout = view.findViewById(R.id.layoutUserMessage)
        val tvUser: TextView = view.findViewById(R.id.tvUserContent)
        val layoutAssistant: LinearLayout = view.findViewById(R.id.layoutAssistantMessage)
        val tvAssistant: TextView = view.findViewById(R.id.tvAssistantContent)
        val tvSources: TextView = view.findViewById(R.id.tvSources)
        val btnTts: ImageButton = view.findViewById(R.id.btnTtsRead)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
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
                onSpeakClicked(msg.content)
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages = messages + message
        notifyItemInserted(messages.size - 1)
    }
}
