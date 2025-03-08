package com.luckyzero.tacotrainer.service

import android.app.Application
import android.util.Log
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.platform.ClockInterface
import com.luckyzero.tacotrainer.platform.assertMainThread
import com.luckyzero.tacotrainer.repositories.SegmentTreeLoader
import com.luckyzero.tacotrainer.viewModels.WorkoutUnroller
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TimerRepository"

@Singleton
class TimerRepository @Inject constructor(
    private val dbAccess: DbAccess,
    private val clock: ClockInterface,
    private val application: Application,
) {
   companion object {
        private const val MAX_TICK_MS = 24L
    }

    private val _workoutFlow = MutableStateFlow<SegmentInterface.Workout?>(null)
    private val _timerStateFlow = MutableStateFlow<TimerState?>(null)
    private var timer: PreloadedTimer? = null
    private var tickJob: Job? = null
    private var autoStart: Boolean = true

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
        assertMainThread()
        val treeLoader = SegmentTreeLoader(dbAccess)
        val tree = treeLoader.loadWorkout(workoutId)
        _workoutFlow.value = tree.workout
        val periodList = WorkoutUnroller.unroll(tree)
        timer = PreloadedTimer(periodList)
        if (autoStart) {
            TimerService.start(application)
        }
    }

    fun clearTimer() {
        assertMainThread()
        timer = null
        _workoutFlow.value = null
    }

    fun start() {
        autoStart = true
        TimerService.start(application)
    }

    fun pause() {
        autoStart = false
        TimerService.pause(application)
    }

    fun resume() {
        autoStart = true
        TimerService.resume(application)
    }

    fun stop() {
        autoStart = false
        TimerService.stop(application)
    }

    // TODO: this this accessible only to the service.
    // This could be done by making the repository implement an interface that has less capability.

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

    fun serviceStart(scope: CoroutineScope) {
        timer?.let {
            it.start(clock.elapsedRealtime())
            startTickJob(it, scope)
        } ?: run {
            error("Timer uninitialized")
        }
    }

    fun servicePause() {
        timer?.let {
            it.pause(clock.elapsedRealtime())
            stopTickJob()
        } ?: run {
            error("Timer uninitialized")
        }
    }

    fun serviceResume(scope: CoroutineScope) {
        timer?.let {
            it.resume(clock.elapsedRealtime())
            startTickJob(it, scope)
        } ?: run {
            error("Timer uninitialized")
        }
    }

    fun serviceStop() {
        timer?.let {
            it.finish()
            stopTickJob()
        } ?: run {
            error("Timer uninitialized")
        }
        onFinished()
    }

    private fun startTickJob(timer: PreloadedTimer, scope: CoroutineScope) {
        if (tickJob == null) {
            tickJob = scope.launch {
                runTicks(timer)
            }
        }
    }

    private fun stopTickJob() {
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

    private fun onFinished() {

    }
}