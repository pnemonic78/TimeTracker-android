package com.tikalk.worktracker.time

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.concurrent.TimeUnit

class TimeListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val projectView: TextView = itemView.findViewById(R.id.project)
    private val taskView: TextView = itemView.findViewById(R.id.task)
    private val startTimeView: TextView = itemView.findViewById(R.id.start_time)
    private val endTimeView: TextView = itemView.findViewById(R.id.end_time)
    private val durationView: TextView = itemView.findViewById(R.id.duration)
    private val noteView: TextView = itemView.findViewById(R.id.note)

    private val recycle = StringBuilder()

    fun bind(record: TimeRecord) {
        val context: Context = itemView.context
        projectView.text = record.project.name
        taskView.text = record.task.name
        val startTime = record.start!!.timeInMillis
        val endTime = record.finish!!.timeInMillis
        startTimeView.text = DateUtils.formatDateTime(context, startTime, DateUtils.FORMAT_SHOW_TIME)
        endTimeView.text = DateUtils.formatDateTime(context, endTime, DateUtils.FORMAT_SHOW_TIME)
        durationView.text = DateUtils.formatElapsedTime(recycle, TimeUnit.MILLISECONDS.toSeconds(endTime - startTime))
        noteView.text = record.note
    }
}