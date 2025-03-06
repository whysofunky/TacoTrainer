package com.luckyzero.tacotrainer.viewModels

import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.SetInstanceInterface
import com.luckyzero.tacotrainer.repositories.SegmentTreeInterface
import java.util.concurrent.TimeUnit

object  WorkoutUnroller{

    data class SetInstance(
        override val set: SegmentInterface.Set,
        override val rep: Int
    ) : SetInstanceInterface

    data class PeriodInstance(
        override val period: SegmentInterface.Period,
        override val set: SetInstanceInterface,
        override val startOffsetMs: Long,
        override val endOffsetMs: Long,
    ) : PeriodInstanceInterface

    fun unroll(segmentTree: SegmentTreeInterface) : List<PeriodInstanceInterface> {
        return unroll(segmentTree.workout, 0)
    }

    private fun unroll(set: SegmentInterface.Set, initialOffsetMs: Long) : List<PeriodInstance> {
        val result = mutableListOf<PeriodInstance>()
        var startOffsetMs: Long = initialOffsetMs
        for (rep in 0 until set.repeatCount) {
            val setInstance = SetInstance(set, rep + 1)
            for (child in set.children) {
                when (child) {
                    is SegmentInterface.Period -> {
                        val endOffsetMs =
                            startOffsetMs + TimeUnit.SECONDS.toMillis(child.totalDuration.toLong())
                        result.add(PeriodInstance(child, setInstance, startOffsetMs, endOffsetMs))
                        startOffsetMs = endOffsetMs
                    }
                    is SegmentInterface.Set -> {
                        result.addAll(unroll(child, startOffsetMs))
                        startOffsetMs = result.lastOrNull()?.endOffsetMs ?: 0
                    }
                    else ->
                        throw IllegalStateException("Unexpected child ${child.javaClass.name}")
                }
            }
        }
        return result
    }
}