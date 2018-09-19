package com.tikalk.worktracker.time

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.TimeRecord

class TimeListAdapter : RecyclerView.Adapter<TimeListViewHolder>() {

    private val records: MutableList<TimeRecord> = ArrayList()

    override fun getItemCount(): Int {
        return records.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeListViewHolder {
        val context: Context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.time_item, parent, false)
        return TimeListViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeListViewHolder, position: Int) {
        val item = records[position]
        holder.bind(item)
    }

    fun set(data: Collection<TimeRecord>) {
        records.clear()
        records.addAll(data)
        notifyDataSetChanged()
    }
}