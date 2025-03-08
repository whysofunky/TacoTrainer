package com.luckyzero.tacotrainer.repositories

import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import kotlinx.coroutines.flow.StateFlow

interface TimerRepository {
    val workoutFlow: StateFlow<SegmentInterface.Workout?>
    val timerStateFlow: StateFlow<TimerState>

    enum class TimerRunState {
        IDLE,
        READY,
        RUNNING,
        PAUSED,
    }

    data class TimerState(
        val runState: TimerRunState,
        val totalElapsedMs: Long?,
        val periodRemainMs: Long?,
        val currentPeriod: PeriodInstanceInterface?,
        val nextPeriod: PeriodInstanceInterface?,
    )

    suspend fun loadTimer(workoutId: Long)
    fun clearTimer()
    fun start()
    fun pause()
    fun resume()
    fun stop()
}