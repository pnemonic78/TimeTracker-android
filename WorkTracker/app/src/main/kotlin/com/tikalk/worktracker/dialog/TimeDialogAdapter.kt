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
import ai.api.model.ResponseMessage
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tikalk.worktracker.R
import java.lang.IllegalArgumentException

class TimeDialogAdapter : RecyclerView.Adapter<TimeDialogViewHolder>() {

    private val data = ArrayList<TikalDialogSpeechBubble>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeDialogViewHolder {
        val context: Context = parent.context
        val view = when (viewType) {
            TikalDialogSpeechBubbleType.ME.ordinal -> {
                LayoutInflater.from(context).inflate(R.layout.speech_bubble_me, parent, false)
            }
            TikalDialogSpeechBubbleType.AGENT.ordinal -> {
                LayoutInflater.from(context).inflate(R.layout.speech_bubble_you, parent, false)
            }
            TikalDialogSpeechBubbleType.ERROR.ordinal -> {
                LayoutInflater.from(context).inflate(R.layout.speech_bubble_error, parent, false)
            }
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
        return TimeDialogViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeDialogViewHolder, position: Int) {
        holder.speech = getItem(position)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return item.type.ordinal
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun getItem(position: Int): TikalDialogSpeechBubble {
        return data[position]
    }

    fun add(response: AIResponse): List<TikalDialogSpeechBubble> {
        val result = response.result
        val bubbles = ArrayList<TikalDialogSpeechBubble>()
        var bubble: TikalDialogSpeechBubble

        bubble = TikalDialogSpeechBubble(TikalDialogSpeechBubbleType.ME, result.resolvedQuery)
        data.add(0, bubble)
        bubbles.add(bubble)

        val fulfillment = result.fulfillment
        val messages = fulfillment.messages

        for (message in messages) {
            if (message is ResponseMessage.ResponseSpeech) {
                for (speech in message.speech) {
                    bubble = TikalDialogSpeechBubble(TikalDialogSpeechBubbleType.AGENT, speech)
                    data.add(0, bubble)
                    bubbles.add(bubble)
                }
            }
        }

        notifyItemRangeChanged(0, bubbles.size)
        return bubbles
    }

    fun add(error: AIError): TikalDialogSpeechBubble {
        val bubble = TikalDialogSpeechBubble(TikalDialogSpeechBubbleType.ERROR, error.message)
        data.add(0, bubble)
        notifyItemInserted(0)
        return bubble
    }
}