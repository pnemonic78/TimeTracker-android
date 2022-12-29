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

import androidx.annotation.MainThread
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.databinding.ComposeItemBinding
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.TimeItem
import com.tikalk.worktracker.time.TimeListAdapter
import com.tikalk.worktracker.time.TimeListViewHolder

class ReportViewHolder(binding: ComposeItemBinding, val filter: ReportFilter) :
    TimeListViewHolder(binding, this) {

    @MainThread
    override fun bind(record: TimeRecord) {
        binding.composeView.setContent {
            TikalTheme {
                TimeItem(
                    record = record,
                    isProjectFieldVisible = filter.isProjectFieldVisible,
                    isTaskFieldVisible = filter.isTaskFieldVisible,
                    isStartFieldVisible = filter.isStartFieldVisible,
                    isFinishFieldVisible = filter.isFinishFieldVisible,
                    isDurationFieldVisible = filter.isDurationFieldVisible,
                    isNoteFieldVisible = filter.isNoteFieldVisible,
                    isCostFieldVisible = filter.isCostFieldVisible,
                    isLocationFieldVisible = filter.isLocationFieldVisible,
                    onClick = clickListener::onRecordClick
                )
            }
        }
    }

    companion object : TimeListAdapter.OnTimeListListener {
        override fun onRecordClick(record: TimeRecord) = Unit

        override fun onRecordSwipe(record: TimeRecord) = Unit
    }
}
