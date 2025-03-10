package com.luckyzero.tacotrainer.service

import android.app.Application
import android.util.Log
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.platform.ClockInterface
import com.luckyzero.tacotrainer.platform.assertMainThread
import com.luckyzero.tacotrainer.repositories.SegmentTreeLoader
import com.luckyzero.tacotrainer.repositories.TimerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        private const val MAX_TICK_MS = 11L

        private fun makeTimerState(
            loading: Boolean,
            timer: WorkoutTimer?
        ): TimerRepository.TimerState {
            val workoutState = when (timer?.state) {
                null -> if (loading)
                    TimerRepository.WorkoutState.LOADING
                else
                    TimerRepository.WorkoutState.NO_WORKOUT
                WorkoutTimer.State.IDLE -> TimerRepository.WorkoutState.READY
                WorkoutTimer.State.RUNNING -> TimerRepository.WorkoutState.RUNNING
                WorkoutTimer.State.PAUSED -> TimerRepository.WorkoutState.PAUSED
                WorkoutTimer.State.FINISHED -> TimerRepository.WorkoutState.FINISHED
            }
            return TimerRepository.TimerState(
                workoutState,
                timer?.timerId,
                timer?.totalElapsedMs,
                timer?.periodRemainMs,
                timer?.currentPeriod(),
                timer?.nextPeriod(),
            )
        }
    }

    private var latestId: Int = 0;
    private var storedTimer: WorkoutTimer? = null
    private var loading: Boolean = false

    private val _workoutFlow = MutableStateFlow<SegmentInterface.Workout?>(null)
    private val _timerStateFlow = MutableStateFlow(makeTimerState(loading, storedTimer))

    override val workoutFlow: StateFlow<SegmentInterface.Workout?> = _workoutFlow
    override val timerStateFlow: StateFlow<TimerRepository.TimerState> = _timerStateFlow

    init {
        Log.d(TAG, "New TimerRunner")
    }

    override suspend fun loadTimer(workoutId: Long) {
        assertMainThread()
        clear()
        setLoading(true)
        val newTimer = try {
            load(workoutId)
        } finally {
            setLoading(false)
        }
        publishTimerState()
    }

    override fun clearTimer() {
        assertMainThread()
        clear()
        publishTimerState()
    }

    override fun start() {
        storedTimer?.let {
            TimerService.start(it.timerId, application)
        }
    }

    override fun pause() {
        storedTimer?.let {
            TimerService.pause(it.timerId, application)
        }
    }

    override fun resume() {
        storedTimer?.let {
            TimerService.resume(it.timerId, application)
        }
    }

    override fun stop() {
        storedTimer?.let {
            TimerService.stop(it.timerId, application)
        }
    }

    override fun restart() {
        storedTimer?.let {
            TimerService.restart(it.timerId, application)
        }
    }

    fun serviceStart(timerId: Int) {
        val t = storedTimer
        if (t?.timerId == timerId) {
            t.start(clock.elapsedRealtime())
        } else{
            Log.i(TAG, "TimerId mismatch operation: start current ${t?.timerId} new $timerId")
        }
    }

    fun servicePause(timerId: Int) {
        val t = storedTimer
        if (t?.timerId == timerId) {
            t.pause(clock.elapsedRealtime())
            publishTimerState()
        } else{
            Log.i(TAG, "TimerId mismatch operation: pause current ${t?.timerId} new $timerId")
        }
    }

    fun serviceResume(timerId: Int) {
        val t = storedTimer
        if (t?.timerId == timerId) {
            t.resume(clock.elapsedRealtime())
        } else{
            Log.i(TAG, "TimerId mismatch operation: resume current ${t?.timerId} new $timerId")
        }
    }

    fun serviceStop(timerId: Int) {
        val t = storedTimer
        if (t?.timerId == timerId) {
            t.stop()
            publishTimerState()
        } else{
            Log.i(TAG, "TimerId mismatch operation: stop current ${t?.timerId} new $timerId")
        }
    }

    fun serviceRestart(timerId: Int) {
        val t = storedTimer
        if (t?.timerId == timerId) {
            t.restart(clock.elapsedRealtime())
        } else{
            Log.i(TAG, "TimerId mismatch operation: restart current ${t?.timerId} new $timerId")
        }
    }

    suspend fun runTicks(timerId: Int) {
        // This should stop when paused, explicitly finished, or just runs out of periods.
        storedTimer?.let { tickingTimer ->
            while (tickingTimer.active() && timerId == storedTimer?.timerId) {
                val msUntilNextPeriod = tickingTimer.onTimeUpdate(clock.elapsedRealtime())
                publishTimerState()
                delay(msUntilNextPeriod.coerceAtMost(MAX_TICK_MS))
            }
            publishTimerState()
        }
    }

    private suspend fun load(workoutId: Long) : WorkoutTimer {
        check(storedTimer == null)
        val treeLoader = SegmentTreeLoader(dbAccess)
        val tree = treeLoader.loadWorkout(workoutId)
        _workoutFlow.value = tree.workout
        val periodList = WorkoutUnroller.unroll(tree)
        latestId += 1
        val newTimer = WorkoutTimer(latestId, periodList)
        storedTimer = newTimer
        return newTimer
    }

    private fun clear() {
        storedTimer?.ensureStopped()
        storedTimer = null
        _workoutFlow.value = null
    }

    private fun setLoading(newValue: Boolean) {
        loading = newValue
        publishTimerState()
    }

    private fun publishTimerState() {
        _timerStateFlow.value = makeTimerState(loading, storedTimer)
    }
}