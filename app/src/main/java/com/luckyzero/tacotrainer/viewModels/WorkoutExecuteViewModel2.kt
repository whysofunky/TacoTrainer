package com.luckyzero.tacotrainer.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.platform.DefaultClock
import com.luckyzero.tacotrainer.repositories.WorkoutTimer
import com.luckyzero.tacotrainer.service.TimerRepository
import com.luckyzero.tacotrainer.service.TimerService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


// Unidirectional flow
// UI -> ViewModel -> Service -> Repository -> ViewModel -> UI

@HiltViewModel
class WorkoutExecuteViewModel2 @Inject constructor (
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

    val stateFlow = repository.timerStateFlow.map {
        when (it?.runState) {
            null -> State.LOADING
            TimerRepository.TimerRunState.READY -> State.READY
            TimerRepository.TimerRunState.RUNNING -> State.RUNNING
            TimerRepository.TimerRunState.PAUSED -> State.PAUSED
            TimerRepository.TimerRunState.FINISHED -> State.FINISHED

        }
    }
    val totalElapsedTimeMsFlow = repository.timerStateFlow.map { it?.totalElapsedMs }
    val periodRemainTimeMsFlow = repository.timerStateFlow.map { it?.periodRemainMs }
    val currentPeriodFlow = repository.timerStateFlow.map { it?.currentPeriod }
    val nextPeriodFlow = repository.timerStateFlow.map { it?.nextPeriod }

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