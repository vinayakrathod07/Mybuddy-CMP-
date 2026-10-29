package com.academialife.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private lateinit var rvNotifs: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnRefresh: TextView
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_notifications, container, false)

        rvNotifs = root.findViewById(R.id.rvNotifications)
        layoutEmpty = root.findViewById(R.id.layoutEmptyNotifs)
        btnRefresh = root.findViewById(R.id.btnRefreshAlerts)

        adapter = NotificationsAdapter(emptyList())
        rvNotifs.layoutManager = LinearLayoutManager(context)
        rvNotifs.adapter = adapter

        btnRefresh.setOnClickListener {
            loadNotifications()
        }

        loadNotifications()
        return root
    }

    private fun loadNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = FirebaseRepository.getNotifications()
            adapter.updateList(list)
            layoutEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}

