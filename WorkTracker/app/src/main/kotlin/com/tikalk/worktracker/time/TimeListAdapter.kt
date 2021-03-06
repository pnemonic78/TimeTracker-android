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
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import com.tikalk.worktracker.databinding.TimeItemBinding
import com.tikalk.worktracker.model.time.TimeRecord

class TimeListAdapter(private val clickListener: OnTimeListListener? = null) :
    ListAdapter<TimeRecord, TimeListViewHolder>(TimeDiffer()) {

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
        val binding = TimeItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return TimeListViewHolder(binding, clickListener)
    }

    override fun onBindViewHolder(holder: TimeListViewHolder, position: Int) {
        holder.record = getItem(position)
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