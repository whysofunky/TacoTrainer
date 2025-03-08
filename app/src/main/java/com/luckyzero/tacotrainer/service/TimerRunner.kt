package com.luckyzero.tacotrainer.service

import android.app.Application
import android.util.Log
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.platform.ClockInterface
import com.luckyzero.tacotrainer.platform.assertMainThread
import com.luckyzero.tacotrainer.repositories.SegmentTreeLoader
import com.luckyzero.tacotrainer.repositories.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TimerRunner"

@Singleton
class TimerRunner @Inject constructor(
    private val dbAccess: DbAccess,
    private val clock: ClockInterface,
    private val application: Application,
) : TimerRepository {
    companion object {
        private const val MAX_TICK_MS = 24L
    }

    private var timer: WorkoutTimer? = null
    private var tickJob: Job? = null
    private var autoStart: Boolean = true
    private var runState: TimerRepository.TimerRunState = TimerRepository.TimerRunState.IDLE

    private val _workoutFlow = MutableStateFlow<SegmentInterface.Workout?>(null)
    private val _timerStateFlow = MutableStateFlow(makeTimerState(runState, timer))

    override val workoutFlow: StateFlow<SegmentInterface.Workout?> = _workoutFlow
    override val timerStateFlow: StateFlow<TimerRepository.TimerState> = _timerStateFlow

    override suspend fun loadTimer(workoutId: Long) {
        assertMainThread()
        val treeLoader = SegmentTreeLoader(dbAccess)
        val tree = treeLoader.loadWorkout(workoutId)
        _workoutFlow.value = tree.workout
        val periodList = WorkoutUnroller.unroll(tree)
        timer = WorkoutTimer(periodList)
        runState = TimerRepository.TimerRunState.READY
        publishTimerState(timer)
        if (autoStart) {
            TimerService.start(application)
        }
    }

    override fun clearTimer() {
        assertMainThread()
        timer = null
        _workoutFlow.value = null
    }

    override fun start() {
        autoStart = true
        TimerService.start(application)
    }

    override fun pause() {
        autoStart = false
        TimerService.pause(application)
    }

    override fun resume() {
        autoStart = true
        TimerService.resume(application)
    }

    override fun stop() {
        autoStart = false
        TimerService.stop(application)
    }

    fun serviceStart(scope: CoroutineScope) {
        timer?.let {
            it.start(clock.elapsedRealtime())
            runState = TimerRepository.TimerRunState.RUNNING
            publishTimerState(it)
            startTickJob(it, scope)
        } ?: run {
            error("Timer uninitialized")
        }
    }

    fun servicePause() {
        timer?.let {
            it.pause(clock.elapsedRealtime())
            runState = TimerRepository.TimerRunState.PAUSED
            publishTimerState(it)
            stopTickJob()
        } ?: run {
            error("Timer uninitialized")
        }
    }

    fun serviceResume(scope: CoroutineScope) {
        timer?.let {
            it.resume(clock.elapsedRealtime())
            runState = TimerRepository.TimerRunState.RUNNING
            publishTimerState(it)
            startTickJob(it, scope)
        } ?: run {
            error("Timer uninitialized")
        }
    }

    fun serviceStop() {
        timer?.finish() ?: run {
            error("Timer uninitialized")
        }
        onFinished()
    }

    private fun publishTimerState(timer: WorkoutTimer?) {
        _timerStateFlow.value = makeTimerState(runState, timer)
    }

    private fun makeTimerState(
        runState: TimerRepository.TimerRunState,
        timer: WorkoutTimer?
    ): TimerRepository.TimerState {
        return TimerRepository.TimerState(
            runState,
            timer?.totalElapsedMs,
            timer?.periodRemainMs,
            timer?.currentPeriod(),
            timer?.nextPeriod(),
        )
    }

    private fun startTickJob(timer: WorkoutTimer, scope: CoroutineScope) {
        if (tickJob == null) {
            tickJob = scope.launch {
                runTicks(timer)
                onFinished()
            }
        }
    }

    private fun stopTickJob() {
        tickJob?.cancel()
        tickJob = null
    }

    private suspend fun runTicks(timer: WorkoutTimer) {
        while (!timer.finished()) {
            val msUntilNextPeriod = timer.onTimeUpdate(clock.elapsedRealtime())
            publishTimerState(timer)
            val delayMs = msUntilNextPeriod.coerceAtMost(MAX_TICK_MS)
            delay(delayMs)
        }
    }

    private fun onFinished() {
        timer = null
        stopTickJob()
        runState = TimerRepository.TimerRunState.IDLE
        publishTimerState(timer)
    }
}