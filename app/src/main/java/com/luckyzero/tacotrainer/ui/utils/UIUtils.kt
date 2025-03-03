package com.luckyzero.tacotrainer.ui.utils

object UIUtils {
    enum class DurationElement {
        NONE,
        SECONDS,
        MINUTES,
        HOURS,
    }
    fun formatDuration(
        durationSeconds: Int,
        minElement: DurationElement = DurationElement.MINUTES
    ) : String {
        val seconds = durationSeconds % 60
        val minutes = (durationSeconds / 60) % 60
        val hours = durationSeconds / 3600
        return if (hours > 0 || minElement >= DurationElement.HOURS) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else if (minutes > 0 || minElement >= DurationElement.MINUTES) {
            "%d:%02d".format(minutes, seconds)
        } else if (seconds > 0 || minElement >= DurationElement.SECONDS) {
            "%d".format(seconds)
        } else {
            ""
        }
    }

}

// TODO: Create an extensions file for this
fun String.removeAll(chars: String): String {
    val sb = StringBuilder()
    this.forEach {
        if (!chars.contains(it)) {
            sb.append(it)
        }
    }
    return sb.toString()
}
