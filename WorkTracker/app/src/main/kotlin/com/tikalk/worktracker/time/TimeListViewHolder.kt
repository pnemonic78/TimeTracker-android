package com.tikalk.worktracker.time

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.*

class TimeListViewHolder(itemView: View, private val clickListener: TimeListAdapter.OnTimeListListener? = null) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

    private val projectView: TextView = itemView.findViewById(R.id.project)
    private val taskView: TextView = itemView.findViewById(R.id.task)
    private val timeRangeView: TextView = itemView.findViewById(R.id.time_range)
    private val noteView: TextView = itemView.findViewById(R.id.note)

    private val timeBuffer = StringBuilder(20)
    private val timeFormatter: Formatter = Formatter(timeBuffer, Locale.getDefault())

    var record: TimeRecord? = null
        set(value) {
            field = value
            bind(value)
        }

    init {
        itemView.setOnClickListener(this)
        // CardView does not handle clicks.
        (itemView as ViewGroup).getChildAt(0).setOnClickListener(this)
    }

    private fun bind(record: TimeRecord?) {
        if (record != null) {
            val context: Context = itemView.context
            projectView.text = record.project.name
            taskView.text = record.task.name
            val startTime = record.start!!.timeInMillis
            val endTime = record.finish!!.timeInMillis
            timeBuffer.delete(0, timeBuffer.length)
            val formatter = DateUtils.formatDateRange(context, timeFormatter, startTime, endTime, DateUtils.FORMAT_SHOW_TIME)
            timeRangeView.text = formatter.out() as CharSequence
            noteView.text = record.note
        } else {
            projectView.text = ""
            taskView.text = ""
            timeRangeView.text = ""
            noteView.text = ""
        }
    }

    override fun onClick(v: View) {
        clickListener?.onRecordClick(record!!)
    }
}