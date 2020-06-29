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
import android.text.format.DateUtils
import android.view.View
import androidx.annotation.MainThread
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.TimeListViewHolder
import kotlinx.android.synthetic.main.time_item.view.*

class ReportViewHolder(itemView: View, val filter: ReportFilter) : TimeListViewHolder(itemView) {

    @MainThread
    override fun bind(record: TimeRecord) {
        super.bind(record)
        val context: Context = itemView.context
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

        bindFilter(filter)
    }

    @MainThread
    override fun clear() {
        super.clear()
        bindFilter(filter)
    }

    @MainThread
    private fun bindFilter(filter: ReportFilter) {
        itemView.project.visibility = if (filter.showProjectField) View.VISIBLE else View.GONE
        itemView.projectIcon.visibility = itemView.project.visibility
        itemView.task.visibility = if (filter.showTaskField) View.VISIBLE else View.GONE
        itemView.taskIcon.visibility = itemView.task.visibility
        itemView.timeRange.visibility = if (filter.showStartField or filter.showFinishField) View.VISIBLE else View.GONE
        itemView.timeRangeIcon.visibility = itemView.timeRange.visibility
        itemView.timeDuration.visibility = if (filter.showDurationField) View.VISIBLE else View.GONE
        itemView.note.visibility = if (filter.showNoteField) View.VISIBLE else View.GONE
        itemView.noteIcon.visibility = itemView.note.visibility
        itemView.cost.visibility = if (filter.showCostField) View.VISIBLE else View.GONE
        itemView.costIcon.visibility = itemView.cost.visibility
    }

    companion object {
        private const val FORMAT_DURATION = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
    }
}
