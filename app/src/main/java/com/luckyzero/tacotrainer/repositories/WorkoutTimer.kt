package com.luckyzero.tacotrainer.repositories

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.platform.ClockInterface
import com.luckyzero.tacotrainer.viewModels.WorkoutUnroller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val TAG = "WorkoutTimer"

// This is a repository, not a viewModel, because we might want the timer to keep running
// even if the user dismisses the UI.
// Actually it should be part of a service.

class WorkoutTimer(
    private val periodList: List<PeriodInstanceInterface>,
    private val clock: ClockInterface,
    private val serviceScope: CoroutineScope,
    ) {

    private var startTimeMs: Long = 0
    private var pauseStartMs: Long? = null
    private var pausedDurationMs: Long = 0

    private val _totalElapsedTimeMs = MutableStateFlow<Long>(0)
    private val _periodRemainTimeMs = MutableStateFlow<Long>(0)
    private val _currentPeriodIdx = MutableStateFlow(0)

    val totalDurationMs: Long = periodList.lastOrNull()?.endOffsetMs ?: 0
    val totalElapsedTimeMsFlow: StateFlow<Long> = _totalElapsedTimeMs
    val periodRemainTimeMsFlow: StateFlow<Long> = _periodRemainTimeMs
    val currentPeriodFlow = _currentPeriodIdx.map {
        if (it < periodList.count()) periodList[it] else null
    }
    val nextPeriodFlow = _currentPeriodIdx.map {
        if (it+1 < periodList.count()) periodList[it+1] else null
    }

    fun start() {
        val timeMs = clock.elapsedRealtime()
        startTimeMs = timeMs
        resumeTick()
    }

    fun pause() {
        if (startTimeMs == 0L) throw IllegalStateException("WorkoutTimer not yet started")
        val timeMs = clock.elapsedRealtime()
        if (pauseStartMs == null) {
            pauseStartMs = timeMs
        }
        stopTick()
    }

    fun resume() {
        if (startTimeMs == 0L) throw IllegalStateException("WorkoutTimer not yet started")
        val timeMs = clock.elapsedRealtime()
        pauseStartMs?.let {
            pausedDurationMs += (timeMs - it)
        }
        pauseStartMs = null
        resumeTick()
    }

    fun currentPeriod() : PeriodInstanceInterface? {
        return _currentPeriodIdx.value.let {
            if (it < periodList.count()) periodList[it] else null
        }
    }

    fun nextPeriod() : PeriodInstanceInterface? {
        return _currentPeriodIdx.value.let {
            if (it+1 < periodList.count()) periodList[it+1] else null
        }
    }

    private var tickJob: Job? = null
    private fun resumeTick() {
        if (tickJob == null) {
            tickJob = serviceScope.launch {
                runTicks()
            }
        }
    }

    private fun stopTick() {
        tickJob?.cancel()
        tickJob = null
    }

    private suspend fun runTicks() {
        while (running()) {
            val timeMs = clock.elapsedRealtime()
            val timeUntilNextSecondMs = onTimeUpdate(timeMs)
            delay(timeUntilNextSecondMs)
        }
        Log.d(TAG, "no longer running")
    }

    private fun running(): Boolean {
        return pauseStartMs == null && currentPeriod() != null
    }

    private fun onTimeUpdate(timeMs: Long) : Long {
        // Normalized time is the elapsed time that the workout has been active, starting at zero,
        // regardless of when it started or how much it was paused.
        val normalizedTime = timeMs - startTimeMs - pausedDurationMs

        _totalElapsedTimeMs.value = normalizedTime

        while (currentPeriod()?.let { it.endOffsetMs <= normalizedTime } == true) {
            _currentPeriodIdx.value += 1
        }
        currentPeriod()?.let {
            _periodRemainTimeMs.value = it.endOffsetMs - normalizedTime
        }

        val millisPerSecond = TimeUnit.SECONDS.toMillis(1)
        val timeUntilNextSecond = millisPerSecond - (normalizedTime % millisPerSecond)
        return timeUntilNextSecond
    }
}