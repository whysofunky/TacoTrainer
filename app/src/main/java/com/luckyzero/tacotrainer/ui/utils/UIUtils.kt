package com.luckyzero.tacotrainer.ui.utils

object UIUtils {
    fun formatDuration(durationSeconds: Int) : String {
        val seconds = durationSeconds % 60
        val minutes = (durationSeconds / 60) % 60
        val hours = durationSeconds / 3600
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}