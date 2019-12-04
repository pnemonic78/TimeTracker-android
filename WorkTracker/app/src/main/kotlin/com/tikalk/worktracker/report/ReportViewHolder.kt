/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * • Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * • Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * • Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tikalk.worktracker.report

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.text.format.DateUtils
import android.view.View
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.formatElapsedTime
import kotlinx.android.synthetic.main.report_item.view.*
import java.util.*

class ReportViewHolder(itemView: View, val filter: ReportFilter) : RecyclerView.ViewHolder(itemView) {

    private val timeBuffer = StringBuilder(20)
    private val timeFormatter: Formatter = Formatter(timeBuffer, Locale.getDefault())
    private val night = (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    var record: TimeRecord? = null
        set(value) {
            field = value
            if (value != null) {
                bind(value)
            } else {
                clear()
            }
        }

    @MainThread
    private fun bind(record: TimeRecord) {
        val context: Context = itemView.context
        itemView.project.text = record.project.name
        itemView.task.text = record.task.name
        val startTime = record.startTime
        val endTime = record.finishTime
        if (filter.showStartField) {
            if (filter.showFinishField) {
                timeBuffer.setLength(0)
                val formatterRange = DateUtils.formatDateRange(context, timeFormatter, startTime, endTime, FORMAT_DURATION)
                itemView.timeRange.text = formatterRange.out() as CharSequence
            } else {
                itemView.timeRange.text = DateUtils.formatDateTime(context, startTime, FORMAT_DURATION)
            }
        } else if (filter.showFinishField) {
            itemView.timeRange.text = DateUtils.formatDateTime(context, endTime, FORMAT_DURATION)
        }
        timeBuffer.setLength(0)
        val formatterElapsed = formatElapsedTime(context, timeFormatter, endTime - startTime)
        itemView.timeDuration.text = formatterElapsed.out() as CharSequence
        itemView.note.text = record.note
        itemView.cost.text = formatCost(record.cost)

        bindColors(record)
        bindFilter()
    }

    @MainThread
    private fun clear() {
        itemView.project.text = ""
        itemView.task.text = ""
        itemView.timeRange.text = ""
        itemView.timeDuration.text = ""
        itemView.note.text = ""
        itemView.cost.text = ""
        bindFilter()
    }

    @MainThread
    private fun bindColors(record: TimeRecord) {
        val projectId = record.project.id
        val taskId = record.task.id
        val mappedId = (projectId * taskId).rem(512).toInt()

        // 512 combinations => 3 bits per color
        val redBits = mappedId.and(7)
        val greenBits = mappedId.shr(3).and(7)
        val blueBits = mappedId.shr(6).and(7)
        val r = redBits * 24 //*32 => some colors too bright
        val g = greenBits * 24 //*32 => some colors too bright
        val b = blueBits * 24 //*32 => some colors too bright
        val color = if (night) Color.rgb(255 - r, 255 - g, 255 - b) else Color.rgb(r, g, b)

        itemView.project.setTextColor(color)
        itemView.task.setTextColor(color)
        itemView.note.setTextColor(color)
        itemView.cost.setTextColor(color)
    }

    @MainThread
    private fun bindFilter() {
        itemView.project.visibility = if (filter.showProjectField) View.VISIBLE else View.GONE
        itemView.task.visibility = if (filter.showTaskField) View.VISIBLE else View.GONE
        itemView.timeRange.visibility = if (filter.showStartField or filter.showFinishField) View.VISIBLE else View.GONE
        itemView.timeDuration.visibility = if (filter.showDurationField) View.VISIBLE else View.GONE
        itemView.note.visibility = if (filter.showNotesField) View.VISIBLE else View.GONE
        itemView.cost.visibility = if (filter.showCostField) View.VISIBLE else View.GONE
    }

    private fun formatCost(cost: Double): CharSequence {
        return if (cost <= 0.0) "" else cost.toString()
    }

    companion object {
        private const val FORMAT_DURATION = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
    }
}
