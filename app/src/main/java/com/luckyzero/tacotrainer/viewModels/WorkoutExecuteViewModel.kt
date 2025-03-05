package com.luckyzero.tacotrainer.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.platform.DefaultClock
import com.luckyzero.tacotrainer.repositories.WorkoutTimer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutExecuteViewModel(
    workoutId: Long,
    dbAccess: DbAccess
): ViewModel() {

    private val clock = DefaultClock

    private val timer = WorkoutTimer(workoutId, dbAccess, clock, viewModelScope)

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