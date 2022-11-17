/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2022, Tikal Knowledge, Ltd.
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

package com.tikalk.worktracker.user

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.tikalk.worktracker.R
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

class UsersScrollerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnScrollIndexListener {
        fun onScrollIndex(index: String)
    }

    var scrollIndexListener: OnScrollIndexListener? = null

    private val indices: MutableList<String> = CopyOnWriteArrayList()
    private val indicesBounds: MutableMap<String, RectF> = mutableMapOf()
    private val textPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)

    init {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        if (typedValue.resourceId != 0) {
            val textColor =
                ResourcesCompat.getColor(context.resources, typedValue.resourceId, theme)
            textPaint.color = textColor
        }

        val res = context.resources
        textPaint.textSize = res.getDimension(R.dimen.index_textSize)
        textPaint.textAlign = Paint.Align.CENTER
    }

    fun setIndices(indices: Collection<String>) {
        this.indices.clear()
        this.indices.addAll(indices.distinct().sorted())
        forceLayout()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val bounds = mutableMapOf<String, RectF>()
        val heightView = (bottom - top).toFloat()
        val height = max(textPaint.textSize, heightView / (indices.size + 1))
        val x1 = 0f
        var y1 = 0f
        val x2 = (right - left).toFloat()
        var y2 = y1 + height

        for (index in indices) {
            val rect = RectF(x1, y1, x2, y2)
            bounds[index] = rect
            y1 = y2
            y2 += height
        }

        indicesBounds.clear()
        indicesBounds.putAll(bounds)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val descent = textPaint.fontMetrics.descent
        for (index in indices) {
            val rect = indicesBounds[index] ?: continue
            canvas.drawText(index, rect.centerX(), rect.centerY() + descent, textPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val y = event.y
            val index = findIndex(y) ?: return false
            scrollIndexListener?.onScrollIndex(index)
        } else if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            val y = event.y
            val index = findIndex(y) ?: return false
            scrollIndexListener?.onScrollIndex(index)
        }
        return true
    }

    private fun findIndex(y: Float): String? {
        indicesBounds.forEach { (index, rect) ->
            if (rect.contains(0f, y)) {
                return index
            }
        }
        return null
    }
}