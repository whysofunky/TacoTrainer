package com.luckyzero.tacotrainer.ui.utils

import android.util.Log

private const val TAG = "UIUtils"

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

    fun millisToDurationSeconds(millis: Long) : Int {
        // We want to round up, so that a time of 12.5 seconds displays as 13.
        // This is because we are counting down how many seconds are left. We don't actually
        // display zero, once you hit zero, the period or workout is over.
        val seconds = (millis/1000).toInt()
        val remainder = millis % 1000
        val v = if (remainder > 0) {
            seconds + 1
        } else {
            seconds
        }
        return v
    }

    fun millisToElapsedSeconds(millis: Long) : Int {
        val v = (millis/1000).toInt()
        // We want to round down, so that an elapsed time of 12.5 seconds displays as 12.
        // This is because the period or workout will often run *slightly* over the requested
        // time, but a few milliseconds. Often less than a frame length, but if we were to round
        // up, then the workout that was intended to take 12 seconds and ran for 12.001 seconds
        // would display a total elapsed of 13.
        return v
    }
}
