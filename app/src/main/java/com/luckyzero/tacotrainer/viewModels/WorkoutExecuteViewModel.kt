package com.luckyzero.tacotrainer.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.platform.DefaultClock
import com.luckyzero.tacotrainer.repositories.SegmentTreeLoader
import com.luckyzero.tacotrainer.repositories.WorkoutTimer
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.switchMap
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

class WorkoutExecuteViewModel(
    workoutId: Long,
    private val segmentTreeLoader: SegmentTreeLoader,
): ViewModel() {

    private val clock = DefaultClock
    private val _workoutFlow = MutableStateFlow<WorkoutInterface?>(null)
    private val _periodListFlow = MutableStateFlow<List<PeriodInstanceInterface>>(emptyList())
    private val _totalDurationMsFlow = MutableStateFlow<Long>(0)
    private val _totalElapsedTimeMsFlow = MutableStateFlow<Long>(0)
    private val _periodRemainTimeMsFlow = MutableStateFlow<Long>(0)
    private val _currentPeriodFlow = MutableStateFlow<PeriodInstanceInterface?>(null)
    private val _nextPeriodFlow = MutableStateFlow<PeriodInstanceInterface?>(null)

    // TODO: I'd like to be able to do it this way, but I can't get the flows to work.
    /*
    private val _timerFlow : Flow<WorkoutTimer> = flow {
        val tree = segmentTreeLoader.loadWorkout(workoutId)
        _workoutFlow.value = ImmutableWorkout(tree.workout)
        val periodList = WorkoutUnroller.unroll(tree)
        _periodListFlow.value = periodList
        val timer = WorkoutTimer(periodList, clock, viewModelScope)
        emit(timer)
    }
*/
    val workoutFlow: StateFlow<WorkoutInterface?> = _workoutFlow
    val periodListFlow: StateFlow<List<PeriodInstanceInterface>> = _periodListFlow

    val totalDurationMs: StateFlow<Long> = _totalDurationMsFlow
    val totalElapsedTimeMsFlow: StateFlow<Long> = _totalElapsedTimeMsFlow
    val periodRemainTimeMsFlow: StateFlow<Long> = _periodRemainTimeMsFlow
    val currentPeriodFlow: StateFlow<PeriodInstanceInterface?> = _currentPeriodFlow
    val nextPeriodFlow: StateFlow<PeriodInstanceInterface?> = _nextPeriodFlow

    private val timerDeferred = viewModelScope.async(Dispatchers.Default, CoroutineStart.LAZY) {
        val tree = segmentTreeLoader.loadWorkout(workoutId)
        _workoutFlow.value = ImmutableWorkout(tree.workout)
        val periodList = WorkoutUnroller.unroll(tree)
        _periodListFlow.value = periodList
        val timer = WorkoutTimer(periodList, clock, viewModelScope)
        _totalDurationMsFlow.value = timer.totalDurationMs
        timer.totalElapsedTimeMsFlow.collectLatest { _totalElapsedTimeMsFlow.value = it }
        timer.periodRemainTimeMsFlow.collectLatest { _periodRemainTimeMsFlow.value = it }
        timer.currentPeriodFlow.collectLatest { _currentPeriodFlow.value = it }
        timer.nextPeriodFlow.collectLatest { _nextPeriodFlow.value = it }
        timer
    }

/*
    I think this is a nicer approach but I can't make it work.
    val totalElapsedTimeMsFlow = _timerFlow.flatMapConcat { it.totalElapsedTimeMsFlow }
    @OptIn(ExperimentalCoroutinesApi::class)
    val periodRemainTimeMsFlow = _timerFlow.map { it.periodRemainTimeMsFlow }.flattenConcat()
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentPeriodFlow = _timerFlow.map { it.currentPeriodFlow }.flattenConcat()
    @OptIn(ExperimentalCoroutinesApi::class)
    val nextPeriodFlow = _timerFlow.map { it.nextPeriodFlow }.flattenConcat()
*/
    fun start() {
        viewModelScope.launch {
            val x = timerDeferred
            val y = x.await()
            val z = start()
//            timerDeferred.await().start()
        }
    }

    fun pause() {
        viewModelScope.launch {
            timerDeferred.await().start()
        }
    }

    fun resume() {
        viewModelScope.launch {
            timerDeferred.await().start()
        }
    }

    private class ImmutableWorkout(
        override val id: Long,
        override val name: String,
        override val repeatCount: Int,
        override val totalDuration: Int,
    ): WorkoutInterface {
        constructor(workout: SegmentInterface.Workout) : this(
            workout.workoutId,
            workout.name,
            workout.repeatCount,
            workout.totalDuration
        )
    }
}