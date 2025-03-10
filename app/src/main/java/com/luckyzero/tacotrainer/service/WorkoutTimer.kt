package com.luckyzero.tacotrainer.service

import android.util.Log
import com.luckyzero.tacotrainer.models.PeriodInstanceInterface

private const val TAG = "WorkoutTimer"

class WorkoutTimer(
    val timerId: Int,
    private val periodList: List<PeriodInstanceInterface>
) {
    enum class State {
        IDLE,
        RUNNING,
        PAUSED,
        FINISHED
    }
    var state: State = State.IDLE
        private set
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
        if (state == State.RUNNING || state == State.PAUSED) error("Starting timer twice")
        if (state == State.FINISHED) error("Already finished")
        state = State.RUNNING
        startTimeMs = clockTimeMs
    }

    fun pause(clockTimeMs: Long) {
        if (state != State.RUNNING) error("Not started")
        state = State.PAUSED
        pausedStartTimeMs = clockTimeMs
    }

    fun resume(clockTimeMs: Long) {
        if (state != State.PAUSED) error("Not currently paused")
        state = State.RUNNING
        pausedDurationMs += (clockTimeMs - (pausedStartTimeMs ?: 0))
    }

    fun ensureStopped() {
        state = State.FINISHED
    }

    fun stop() {
        if (state != State.RUNNING && state != State.PAUSED) error("Not running: state $state")
        state = State.FINISHED
    }

    fun restart(clockTimeMs: Long) {
        // The same as Start, but doesn't worry about the finished state.
        if (state == State.RUNNING || state == State.PAUSED) error("Starting timer twice")
        if (state == State.IDLE) error("Never was started")
        state = State.RUNNING
        startTimeMs = clockTimeMs
        totalElapsedMs = 0
        periodRemainMs = 0
        pausedStartTimeMs = null
        pausedDurationMs = 0
        currentIdx = 0
    }

    fun active() : Boolean {
        return state == State.RUNNING
    }

    // Returns milliseconds until next period ends.
    fun onTimeUpdate(clockTimeMs: Long) : Long {
        val newElapsedTime = clockTimeMs - startTimeMs - pausedDurationMs
        val nextPeriod = updateElapsedTime(newElapsedTime)
        return nextPeriod
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
            state = State.FINISHED
            periodRemainMs = 0
        }
        return periodRemainMs
    }
}