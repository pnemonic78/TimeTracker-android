package com.tikalk.worktracker.time

import android.content.Context
import android.graphics.Color
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
            if (value != null) {
                bind(value)
            } else {
                clear()
            }
        }

    init {
        itemView.setOnClickListener(this)
        // CardView does not handle clicks.
        (itemView as ViewGroup).getChildAt(0).setOnClickListener(this)
    }

    private fun bind(record: TimeRecord) {
        val context: Context = itemView.context
        projectView.text = record.project.name
        taskView.text = record.task.name
        val startTime = record.start!!.timeInMillis
        val endTime = record.finish!!.timeInMillis
        timeBuffer.delete(0, timeBuffer.length)
        val formatter = DateUtils.formatDateRange(context, timeFormatter, startTime, endTime, DateUtils.FORMAT_SHOW_TIME)
        timeRangeView.text = formatter.out() as CharSequence
        noteView.text = record.note

        bindColors(record)
    }

    private fun clear() {
        projectView.text = ""
        taskView.text = ""
        timeRangeView.text = ""
        noteView.text = ""
    }

    private fun bindColors(record: TimeRecord) {
        val projectId = record.project.id
        val taskId = record.task.id
        val mappedId = (projectId * taskId).rem(512).toInt()

        // 512 combinations => 3 bits per color
        val redBits = mappedId.and(7)
        val greenBits = mappedId.shr(3).and(7)
        val blueBits = mappedId.shr(6).and(7)
        val r = redBits * 32
        val g = greenBits * 32
        val b = blueBits * 32
        val color = Color.rgb(r, g, b)

        projectView.setTextColor(color)
        taskView.setTextColor(color)
        noteView.setTextColor(color)
    }

    override fun onClick(v: View) {
        clickListener?.onRecordClick(record!!)
    }
}