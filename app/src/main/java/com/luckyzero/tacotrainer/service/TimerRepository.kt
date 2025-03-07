package com.luckyzero.tacotrainer.service

import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.platform.ClockInterface
import com.luckyzero.tacotrainer.repositories.SegmentTreeLoader
import com.luckyzero.tacotrainer.repositories.WorkoutTimer.State
import com.luckyzero.tacotrainer.viewModels.WorkoutUnroller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class TimerRepository @Inject constructor(
    private val dbAccess: DbAccess,
    private val clock: ClockInterface
) {
    companion object {
        private const val MAX_TICK_MS = 24L
    }

    private val _workoutFlow = MutableStateFlow<SegmentInterface.Workout?>(null)
    private val _timerStateFlow = MutableStateFlow<TimerState?>(null)
    private var preloadedTimer: PreloadedTimer? = null
    private var tickerRunner: TickRunner? = null

    val workoutFlow: StateFlow<SegmentInterface.Workout?> = _workoutFlow
    val timerStateFlow: StateFlow<TimerState?> = _timerStateFlow

    enum class TimerRunState {
        READY,
        RUNNING,
        PAUSED,
        FINISHED
    }

    data class TimerState(
        val runState: TimerRunState,
        val totalElapsedMs: Long,
        val periodRemainMs: Long,
        val currentPeriod: PeriodInstanceInterface?,
        val nextPeriod: PeriodInstanceInterface?,
    )

    suspend fun loadTimer(workoutId: Long) {
        val treeLoader = SegmentTreeLoader(dbAccess)
        val tree = treeLoader.loadWorkout(workoutId)
        _workoutFlow.value = tree.workout
        val periodList = WorkoutUnroller.unroll(tree)
        preloadedTimer = PreloadedTimer(periodList)
    }

    fun clearTimer() {
        if (tickerRunner != null) {
            abort()
        }
        tickerRunner = null
        preloadedTimer = null
        _workoutFlow.value = null
    }

    fun start(scope: CoroutineScope) {
        preloadedTimer?.let { timer ->
            tickerRunner = TickRunner(timer, clock, scope).also { it.start() }
        } ?: run {
            throw IllegalStateException("No timer loaded")
        }
    }

    fun pause() {
        tickerRunner?.pause() ?: run {
            throw IllegalStateException("No tick runner")
        }
    }

    fun resume() {
        tickerRunner?.resume() ?: run {
            throw IllegalStateException("No tick runner")
        }
    }

    fun abort() {
        tickerRunner?.abort() ?: run {
            throw IllegalStateException("No tick runner")
        }
    }

    private fun onFinished() {
        tickerRunner = null
        preloadedTimer = null
    }

    private fun publishTimerState(timer: PreloadedTimer?) {
        timer ?: run {
            _timerStateFlow.value = null
            return
        }
        val runState = when (timer.state) {
            PreloadedTimer.State.IDLE -> TimerRunState.READY
            PreloadedTimer.State.RUNNING -> TimerRunState.RUNNING
            PreloadedTimer.State.PAUSED -> TimerRunState.PAUSED
            PreloadedTimer.State.FINISHED -> TimerRunState.FINISHED
        }
        val timerState = TimerState(
            runState,
            timer.totalElapsedMs,
            timer.periodRemainMs,
            timer.currentPeriod(),
            timer.nextPeriod(),
        )
        _timerStateFlow.value = timerState
    }

    private inner class TickRunner(
        private val timer: PreloadedTimer,
        private val clock: ClockInterface,
        private val scope: CoroutineScope,
    ) {
        private var tickJob: Job? = null

        fun start() {
            timer.start(clock.elapsedRealtime())
            resumeTicks()
        }

        fun pause() {
            stopTicks()
        }

        fun resume() {
            resumeTicks()
        }

        fun abort() {
            stopTicks()
            timer.finish()
            onFinished()
        }

        private fun resumeTicks() {
            if (tickJob == null) {
                tickJob = scope.launch {
                    runTicks(timer)
                }
            }
        }

        private fun stopTicks() {
            tickJob?.cancel()
            tickJob = null
        }

        private suspend fun runTicks(timer: PreloadedTimer) {
            while (!timer.done()) {
                val msUntilNextPeriod = timer.onTimeUpdate(clock.elapsedRealtime())
                publishTimerState(timer)
                val delayMs = msUntilNextPeriod.coerceAtMost(MAX_TICK_MS)
                delay(delayMs)
            }
            onFinished()
        }
    }

}