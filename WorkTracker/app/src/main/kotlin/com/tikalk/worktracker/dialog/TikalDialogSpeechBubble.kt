package com.tikalk.worktracker.dialog

enum class TikalDialogSpeechBubbleType {
    ME,
    AGENT,
    ERROR
}

data class TikalDialogSpeechBubble(val type: TikalDialogSpeechBubbleType, val speech: String)