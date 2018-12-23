package com.tikalk.worktracker.time

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.TimeRecord

class TimeListAdapter(private val clickListener: OnTimeListListener? = null) : ListAdapter<TimeRecord, TimeListViewHolder>(TimeDiffer()) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeListViewHolder {
        val context: Context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.time_item, parent, false)
        return TimeListViewHolder(view, clickListener)
    }

    override fun onBindViewHolder(holder: TimeListViewHolder, position: Int) {
        val record = getItem(position)
        holder.record = record
    }

    private class TimeDiffer : ItemCallback<TimeRecord>() {
        override fun areItemsTheSame(oldItem: TimeRecord, newItem: TimeRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TimeRecord, newItem: TimeRecord): Boolean {
           return oldItem == newItem
        }
    }
}