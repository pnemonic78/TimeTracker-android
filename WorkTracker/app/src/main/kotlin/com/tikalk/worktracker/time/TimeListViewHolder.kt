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
package com.tikalk.worktracker.time

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import kotlinx.android.synthetic.main.time_item.view.*
import java.util.*

open class TimeListViewHolder(itemView: View, private val clickListener: TimeListAdapter.OnTimeListListener? = null) : RecyclerView.ViewHolder(itemView),
    View.OnClickListener {

    protected val timeBuffer = StringBuilder(20)
    protected val timeFormatter: Formatter = Formatter(timeBuffer, Locale.getDefault())
    protected val night = (itemView.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    var record: TimeRecord? = null
        set(value) {
            field = value
            if (value != null) {
                bind(value)
                bindColors(value)
            } else {
                clear()
            }
        }

    init {
        itemView.setOnClickListener(this)
        // CardView does not handle clicks.
        (itemView as ViewGroup).getChildAt(0).setOnClickListener(this)
    }

    @MainThread
    protected open fun bind(record: TimeRecord) {
        val context: Context = itemView.context
        itemView.project.text = record.project.name
        itemView.task.text = record.task.name
        val startTime = record.startTime
        val endTime = record.finishTime
        timeBuffer.setLength(0)
        val formatterRange = DateUtils.formatDateRange(context, timeFormatter, startTime, endTime, FORMAT_DURATION)
        itemView.timeRange.text = formatterRange.out() as CharSequence
        timeBuffer.setLength(0)
        val formatterElapsed = formatElapsedTime(context, timeFormatter, endTime - startTime)
        itemView.timeDuration.text = formatterElapsed.out() as CharSequence
        itemView.note.text = record.note
        itemView.cost.text = formatCost(record.cost)
        itemView.remoteIcon.visibility = if (record.remote.toBoolean()) View.VISIBLE else View.INVISIBLE
    }

    @MainThread
    protected open fun clear() {
        itemView.project.text = ""
        itemView.task.text = ""
        itemView.timeRange.text = ""
        itemView.timeDuration.text = ""
        itemView.note.text = ""
        itemView.cost.text = ""
        itemView.remoteIcon.visibility = View.INVISIBLE
    }

    @MainThread
    private fun bindColors(record: TimeRecord) {
        val projectHash: Int = if (record.project.id != TikalEntity.ID_NONE) record.project.id.toInt() else record.project.hashCode()
        val taskHash: Int = if (record.task.id != TikalEntity.ID_NONE) record.task.id.toInt() else record.task.hashCode()
        val spread = (projectHash * projectHash * taskHash)
        val spreadBits = spread.and(511)

        // 512 combinations => 3 bits per color
        val redBits = spreadBits.and(0x07)
        val greenBits = spreadBits.shr(3).and(0x07)
        val blueBits = spreadBits.shr(6).and(0x07)
        val r = redBits * 24 //*32 => some colors too bright
        val g = greenBits * 24 //*32 => some colors too bright
        val b = blueBits * 24 //*32 => some colors too bright
        val color = if (night) Color.rgb(255 - r, 255 - g, 255 - b) else Color.rgb(r, g, b)

        bindColors(record, color)
    }

    @MainThread
    protected open fun bindColors(record: TimeRecord, color: Int) {
        itemView.project.setTextColor(color)
        itemView.task.setTextColor(color)
        itemView.note.setTextColor(color)
        itemView.cost.setTextColor(color)
    }

    private fun formatCost(cost: Double): CharSequence {
        return if (cost <= 0.0) "" else cost.toString()
    }

    override fun onClick(v: View) {
        val record = this.record
        if (record != null) {
            clickListener?.onRecordClick(record)
        }
    }

    companion object {
        private const val FORMAT_DURATION = DateUtils.FORMAT_SHOW_TIME
    }
}