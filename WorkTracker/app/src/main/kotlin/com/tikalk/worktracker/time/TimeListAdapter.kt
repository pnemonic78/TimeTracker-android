package com.tikalk.worktracker.time

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.TimeRecord

class TimeListAdapter(private val clickListener: OnTimeListListener? = null) : RecyclerView.Adapter<TimeListViewHolder>() {

    interface OnTimeListListener {
        fun onTimeItemClicked(record: TimeRecord)
    }

    private val records: MutableList<TimeRecord> = ArrayList()

    override fun getItemCount(): Int {
        return records.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeListViewHolder {
        val context: Context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.time_item, parent, false)
        return TimeListViewHolder(view, clickListener)
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