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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@HiltViewModel(assistedFactory = WorkoutExecuteViewModel.Factory::class)
class WorkoutExecuteViewModel @AssistedInject constructor (
    @Assisted workoutId: Long,
    dbAccess: DbAccess,
    application: Application,
): AndroidViewModel(application) {

    @AssistedFactory
    interface Factory {
        fun create(workoutId: Long) : WorkoutExecuteViewModel
    }

    private val timer = WorkoutTimer(workoutId, dbAccess, DefaultClock, viewModelScope)

    val workoutFlow: Flow<WorkoutInterface?> = timer.workoutFlow.map { workout ->
        workout?.let {
            ImmutableWorkout(it)
        }
    }
    val stateFlow = timer.stateFlow
    val totalDurationMs = timer.totalDurationMsFlow
    val totalElapsedTimeMsFlow = timer.totalElapsedTimeMsFlow
    val periodRemainTimeMsFlow = timer.periodRemainTimeMsFlow
    val currentPeriodFlow = timer.currentPeriodFlow
    val nextPeriodFlow = timer.nextPeriodFlow

    fun start() {
        timer.start()
    }

    fun pause() {
        timer.pause()
    }

    fun resume() {
        timer.resume()
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