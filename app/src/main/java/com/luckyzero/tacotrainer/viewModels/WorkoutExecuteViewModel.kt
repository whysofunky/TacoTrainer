package com.luckyzero.tacotrainer.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.repositories.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutExecuteViewModel @Inject constructor (
    private val repository: TimerRepository,
    application: Application,
): AndroidViewModel(application) {

    enum class State {
        IDLE,
        LOADING,
        READY,
        RUNNING,
        PAUSED,
        FINISHED,
    }

    val workoutFlow: Flow<WorkoutInterface?> = repository.workoutFlow.map { workout ->
        workout?.let {
            ImmutableWorkout(it)
        }
    }

    // TODO: I can't tell the difference between IDLE and LOADING.
    // The repository needs to provide this information
    val stateFlow = repository.timerStateFlow.map {
        when (it.runState) {
            TimerRepository.TimerRunState.IDLE -> {
                if (highestState == TimerRepository.TimerRunState.RUNNING)
                    State.FINISHED
                else
                    State.IDLE
            }
            TimerRepository.TimerRunState.READY -> State.READY
            TimerRepository.TimerRunState.RUNNING -> State.RUNNING
            TimerRepository.TimerRunState.PAUSED -> State.PAUSED
        }
    }
    val totalElapsedTimeMsFlow = repository.timerStateFlow.map { it.totalElapsedMs }
    val periodRemainTimeMsFlow = repository.timerStateFlow.map { it.periodRemainMs }
    val currentPeriodFlow = repository.timerStateFlow.map { it.currentPeriod }
    val nextPeriodFlow = repository.timerStateFlow.map { it.nextPeriod }

    private var highestState = TimerRepository.TimerRunState.IDLE

    init {

        viewModelScope.launch {
            repository.timerStateFlow.collect {
                if (highestState < it.runState) {
                    highestState = it.runState
                }
            }
        }
    }

    fun loadWorkout(workoutId: Long) {
        viewModelScope.launch {
            repository.loadTimer(workoutId)
        }
    }

    fun start() {
        repository.start()
    }

    fun pause() {
        repository.pause()
    }

    fun resume() {
        repository.resume()
    }

    fun stop() {
        repository.stop()
        repository.clearTimer()
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