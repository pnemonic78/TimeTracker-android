package com.tikalk.worktracker.dialog

import ai.api.ui.AIButton
import android.content.Context
import android.util.AttributeSet

class TikalDialogButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AIButton(context, attrs, defStyleAttr) {

    override fun onListeningFinished() {
        post { changeState(MicState.normal) }
    }
}