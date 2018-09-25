package com.tikalk.worktracker.time

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.TimeRecord

class TimeListAdapter(private val clickListener: OnTimeListListener? = null) : RecyclerView.Adapter<TimeListViewHolder>() {

    interface OnTimeListListener {
        /**
         * Callback to be invoked when an item in this list has been clicked.
         */
        fun onRecordClick(record: TimeRecord)

        /**
         * Callback to be invoked when an item in this list has been swiped.
         */
        fun onRecordSwipe(record: TimeRecord)
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
        val record = records[position]
        holder.record = record
    }

    fun set(data: Collection<TimeRecord>) {
        records.clear()
        records.addAll(data)
        notifyDataSetChanged()
    }

    fun delete(record: TimeRecord) {
        val index = records.indexOf(record)
        if (index >= 0) {
            records.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}