package com.luckyzero.tacotrainer.service

import android.util.Log
import com.luckyzero.tacotrainer.models.PeriodInstanceInterface

private const val TAG = "WorkoutTimer"

class WorkoutTimer(
    private val periodList: List<PeriodInstanceInterface>
) {
    private var started: Boolean = false
    private var running: Boolean = false
    private var finished: Boolean = false

    var totalElapsedMs: Long = 0
        private set
    var periodRemainMs: Long = 0
        private set

    private var startTimeMs : Long = 0
    private var pausedStartTimeMs : Long? = null
    private var pausedDurationMs : Long = 0
    private var currentIdx: Int = 0

    fun currentPeriod() : PeriodInstanceInterface? {
        return periodList.getOrNull(currentIdx)
    }

    fun nextPeriod() : PeriodInstanceInterface? {
        return periodList.getOrNull(currentIdx + 1)
    }

    fun start(clockTimeMs: Long) {
        if (started) error("Starting timer twice")
        if (finished) error("Already finished")
        started = true
        running = true
        startTimeMs = clockTimeMs
    }

    fun pause(clockTimeMs: Long) {
        if (!started) error("Not started")
        if (!running) return
        pausedStartTimeMs = clockTimeMs
        running = false
        Log.d(TAG, "pause $pausedStartTimeMs")
    }

    fun resume(clockTimeMs: Long) {
        if (!started) error("Not started")
        if (running) return
        pausedDurationMs += (clockTimeMs - (pausedStartTimeMs ?: 0))
        running = true
        Log.d(TAG, "resume $pausedDurationMs")
    }

    fun finish() {
        if (!started) error("Not started")
        running = false
        finished = true
    }

    fun finished() : Boolean {
        return finished
    }

    // Returns milliseconds until next period ends.
    fun onTimeUpdate(clockTimeMs: Long) : Long {
        val newElapsedTime = clockTimeMs - startTimeMs - pausedDurationMs
        Log.d(TAG, "onTimeUpdate $clockTimeMs start $startTimeMs paused $pausedDurationMs elapsed $newElapsedTime")
        return updateElapsedTime(newElapsedTime)
    }

    private fun updateElapsedTime(newElapsedTime: Long) : Long {
        if (newElapsedTime < totalElapsedMs) {
            error("Time should not go backwards")
        }
        totalElapsedMs = newElapsedTime

        while (currentPeriod()?.let { it.endOffsetMs <= newElapsedTime } == true) {
            currentIdx += 1
        }
        currentPeriod()?.let {
            periodRemainMs = it.endOffsetMs - newElapsedTime
        } ?: run {
            finished = true
            periodRemainMs = 0
        }
        return periodRemainMs
    }
}