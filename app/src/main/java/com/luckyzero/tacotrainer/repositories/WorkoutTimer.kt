package com.luckyzero.tacotrainer.repositories

import android.util.Log
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.platform.ClockInterface
import com.luckyzero.tacotrainer.viewModels.WorkoutUnroller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val TAG = "WorkoutTimer"

private const val MAX_TICK_DELAY = 16L

// This is a repository, not a viewModel, because we might want the timer to keep running
// even if the user dismisses the UI.
// Actually it should be part of a service.

class WorkoutTimer(
    private val workoutId: Long,
    private var dbAccess: DbAccess,
    private val clock: ClockInterface,
    private val serviceScope: CoroutineScope,
    ) {

    enum class State {
        IDLE,
        LOADING_READY,
        LOADED_WAITING,
        RUNNING,
        PAUSED,
        FINISHED,
    }

    private var startTimeMs: Long = 0
    private var pauseStartMs: Long? = null
    private var pausedDurationMs: Long = 0

    private val _stateFlow = MutableStateFlow(State.IDLE)
    private val _workoutFlow = MutableStateFlow<SegmentInterface.Workout?>(null)
    private val _periodListFlow = MutableStateFlow<List<PeriodInstanceInterface>?>(null)
    private val _totalElapsedTimeMsFlow = MutableStateFlow<Long>(0)
    private val _periodRemainTimeMsFlow = MutableStateFlow<Long>(0)
    private val _currentPeriodIdxFlow = MutableStateFlow(0)

    val stateFlow: StateFlow<State> = _stateFlow
    val workoutFlow: StateFlow<SegmentInterface.Workout?> = _workoutFlow
    val totalElapsedTimeMsFlow: StateFlow<Long> = _totalElapsedTimeMsFlow
    val periodRemainTimeMsFlow: StateFlow<Long> = _periodRemainTimeMsFlow
    val periodLisFlow: StateFlow<List<PeriodInstanceInterface>?> = _periodListFlow
    val totalDurationMsFlow = _periodListFlow.map { it?.lastOrNull()?.endOffsetMs }

    val currentPeriodFlow = combine(_periodListFlow, _currentPeriodIdxFlow) { periodList, index ->
        periodList?.let { if (index < it.count()) it[index] else null }
    }
    val nextPeriodFlow = combine(_periodListFlow, _currentPeriodIdxFlow) { periodList, index ->
        periodList?.let { if (index+1 < it.count()) it[index+1] else null }
    }
    private var tickJob: Job? = null

    init {
        serviceScope.launch {
            val segmentTreeLoader = SegmentTreeLoader(dbAccess)
            val tree = segmentTreeLoader.loadWorkout(workoutId)
            _workoutFlow.value = tree.workout
            val periodList = WorkoutUnroller.unroll(tree)
            _periodListFlow.value = periodList
            when (val state = stateFlow.value) {
                State.IDLE -> _stateFlow.value = State.LOADED_WAITING
                State.LOADING_READY -> {
                    _stateFlow.value = State.RUNNING
                    startTick()
                }
                else -> throw IllegalStateException("Invalid transition $state to RUNNING")
            }
        }
    }

    fun start() {
        when (val state = stateFlow.value) {
            State.IDLE -> _stateFlow.value = State.LOADING_READY
            State.LOADING_READY -> { return /* no op */ }
            State.RUNNING -> { return /* no op */ }
            State.LOADED_WAITING -> {
                _stateFlow.value = State.RUNNING
                startTick()
            }
            else -> throw IllegalStateException("Invalid transition $state to RUNNING")
        }
    }

    fun pause() {
        when (val state = stateFlow.value) {
            State.RUNNING -> {
                _stateFlow.value = State.PAUSED
                pauseStartMs = clock.elapsedRealtime()
                stopTick()
            }
            State.PAUSED -> { return /* no op */ }
            else -> throw IllegalStateException("Invalid transition $state to PAUSED")
        }
    }

    fun resume() {
        when (val state = stateFlow.value) {
            State.PAUSED -> {
                _stateFlow.value = State.RUNNING
                pauseStartMs?.let {
                    pausedDurationMs += (clock.elapsedRealtime() - it)
                }
                pauseStartMs = null
                resumeTick()
            }
            State.RUNNING -> { return /* no op */ }
            else -> throw IllegalStateException("Invalid transition $state to RUNNING")
        }
    }

    fun currentPeriod() : PeriodInstanceInterface? {
        return _periodListFlow.value?.let { periodList ->
            _currentPeriodIdxFlow.value.let { idx ->
                if (idx < periodList.count()) periodList[idx] else null
            }
        }
    }

    fun nextPeriod() : PeriodInstanceInterface? {
        return _periodListFlow.value?.let { periodList ->
            _currentPeriodIdxFlow.value.let { idx ->
                if (idx+1 < periodList.count()) periodList[idx+1] else null
            }
        }
    }

    private fun startTick() {
        _stateFlow.value = State.RUNNING
        startTimeMs = clock.elapsedRealtime()
        resumeTick()
    }

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
        do {
            val timeUntilNextEvent = onTimeUpdate(clock.elapsedRealtime())
            if (running()) delay(timeUntilNextEvent.coerceAtMost(MAX_TICK_DELAY))
        } while (running())
        _stateFlow.value = State.FINISHED
    }

    private fun running(): Boolean {
        return pauseStartMs == null && currentPeriod() != null
    }

    private fun onTimeUpdate(timeMs: Long) : Long {
        // Normalized time is the elapsed time that the workout has been active, starting at zero,
        // regardless of when it started or how much it was paused.
        val normalizedTime = timeMs - startTimeMs - pausedDurationMs

        _totalElapsedTimeMsFlow.value = normalizedTime

        while (currentPeriod()?.let { it.endOffsetMs <= normalizedTime } == true) {
            _currentPeriodIdxFlow.value += 1
        }
        currentPeriod()?.let {
            _periodRemainTimeMsFlow.value = it.endOffsetMs - normalizedTime
        } ?: run {
            _periodRemainTimeMsFlow.value = 0
        }

        val millisPerSecond = TimeUnit.SECONDS.toMillis(1)
        val timeUntilNextSecond = millisPerSecond - (normalizedTime % millisPerSecond)
        return timeUntilNextSecond
    }
}