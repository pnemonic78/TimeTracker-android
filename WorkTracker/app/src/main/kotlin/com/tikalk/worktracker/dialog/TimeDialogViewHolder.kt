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
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TimeDialogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val meView: TextView = itemView.findViewById(android.R.id.text1)
    private val youView: TextView? = itemView.findViewById(android.R.id.text2)

    var response: Any? = null
        set(value) {
            field = value
            if (value != null) {
                bind(value)
            } else {
                clear()
            }
        }

    private fun bind(item: Any) {
        if (item is AIResponse) {
            bindResponse(item)
        } else if (item is AIError) {
            bindError(item)
        } else {
            clear()
        }
    }

    private fun bindResponse(response: AIResponse) {
        val result = response.result

        meView.text = result.resolvedQuery

        val fulfillment = result.fulfillment
        val speech = fulfillment.speech
        val messages = fulfillment.messages
        var message: String? = null
        if (messages.isNotEmpty()) {
            val responseMessages = messages[0]
            if (responseMessages is ResponseMessage.ResponseSpeech) {
                val speeches = responseMessages.speech
                if (speeches.isNotEmpty()) {
                    message = speeches[0]
                }
            }
        }

        if (message != null) {
            youView!!.text = message
        } else {
            youView!!.text = speech
        }
    }

    private fun bindError(error: AIError) {
        meView.text = error.message
    }

    private fun clear() {
        meView.text = ""
        youView?.text = ""
    }
}