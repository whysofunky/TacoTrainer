package com.luckyzero.tacotrainer.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.repositories.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val workoutFlow: StateFlow<WorkoutInterface?> = repository.workoutFlow
        .mapLatest { workout ->
            workout.toImmutableWorkout()
        }
        .stateIn(viewModelScope,
            SharingStarted.WhileSubscribed(),
            repository.workoutFlow.value.toImmutableWorkout()
        )

    val stateFlow = repository.timerStateFlow.map {
        when (it.workoutState) {
            TimerRepository.WorkoutState.NO_WORKOUT -> State.IDLE
            TimerRepository.WorkoutState.LOADING -> State.LOADING
            TimerRepository.WorkoutState.READY -> State.READY
            TimerRepository.WorkoutState.RUNNING -> State.RUNNING
            TimerRepository.WorkoutState.PAUSED -> State.PAUSED
            TimerRepository.WorkoutState.FINISHED -> State.FINISHED
        }
    }
    val totalElapsedTimeMsFlow = repository.timerStateFlow.map { it.totalElapsedMs }
    val periodRemainTimeMsFlow = repository.timerStateFlow.map { it.periodRemainMs }
    val currentPeriodFlow = repository.timerStateFlow.map { it.currentPeriod }
    val nextPeriodFlow = repository.timerStateFlow.map { it.nextPeriod }

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
    }

    fun restart() {
        repository.restart()
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

    private fun SegmentInterface.Workout?.toImmutableWorkout() : ImmutableWorkout? {
        return this?.let { ImmutableWorkout(it) }
    }
}

