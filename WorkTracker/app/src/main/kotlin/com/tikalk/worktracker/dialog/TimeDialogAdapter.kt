/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
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
package com.tikalk.worktracker.dialog

import ai.api.model.AIError
import ai.api.model.AIResponse
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.R

class TimeDialogAdapter : RecyclerView.Adapter<TimeDialogViewHolder>() {

    companion object {
        private const val TYPE_RESPONSE = 0
        private const val TYPE_ERROR = 1
    }

    private val data = ArrayList<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeDialogViewHolder {
        val context: Context = parent.context
        val view = if (viewType == TYPE_ERROR) {
            LayoutInflater.from(context).inflate(R.layout.speech_bubble_error, parent, false)
        } else {
            LayoutInflater.from(context).inflate(R.layout.speech_bubbles, parent, false)
        }
        return TimeDialogViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeDialogViewHolder, position: Int) {
        holder.response = getItem(position)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        if (item is AIError) {
            return TYPE_ERROR
        }
        return TYPE_RESPONSE
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun getItem(position: Int): Any {
        return data[position]
    }

    fun add(response: AIResponse) {
        data.add(0, response)
        notifyItemInserted(0)
    }

    fun add(error: AIError) {
        data.add(0, error)
        notifyItemInserted(0)
    }
}