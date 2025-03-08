package com.luckyzero.tacotrainer.service

import com.luckyzero.tacotrainer.models.PeriodInstanceInterface

private const val TAG = "WorkoutTimer"

class WorkoutTimer(
    private val periodList: List<PeriodInstanceInterface>
) {
    enum class State {
        IDLE,
        RUNNING,
        PAUSED,
        FINISHED
    }

    var state : State = State.IDLE
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

    fun done() : Boolean {
        return state == State.FINISHED
    }

    fun start(clockTimeMs: Long) {
        if (state != State.IDLE) {
            error("Invalid transition $state to RUNNING")
        }
        state = State.RUNNING
        startTimeMs = clockTimeMs
    }

    fun pause(clockTimeMs: Long) {
        if (state != State.RUNNING) {
            error("Invalid transition $state to PAUSED")
        }
        state = State.PAUSED
        pausedStartTimeMs = clockTimeMs
    }

    fun resume(clockTimeMs: Long) {
        if (state != State.PAUSED) {
            error("Invalid transition $state to RUNNING")
        }
        state = State.RUNNING
        pausedDurationMs += (clockTimeMs - (pausedStartTimeMs ?: 0))
    }

    fun finish() {
        if (state != State.RUNNING && state != State.PAUSED) {
            error("Invalid transition $state to FINISHED")
        }
        state = State.FINISHED
    }

    // Returns milliseconds until next period ends.
    fun onTimeUpdate(clockTimeMs: Long) : Long {
        if (state == State.FINISHED) {
            return 0
        }
        val newElapsedTime = clockTimeMs - startTimeMs - pausedDurationMs
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
            periodRemainMs = 0
        }
        return periodRemainMs
    }
}