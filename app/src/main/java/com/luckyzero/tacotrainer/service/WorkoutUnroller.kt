package com.luckyzero.tacotrainer.service

import com.luckyzero.tacotrainer.models.PeriodInstanceInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.repositories.SegmentTreeInterface
import java.util.concurrent.TimeUnit

object  WorkoutUnroller{

    data class PeriodInstance(
        override val segmentId: Long,
        override val name: String,
        override val duration: Int,
        override val startOffsetMs: Long,
        override val endOffsetMs: Long,
        override val repetition: Int,
        override val setRepeatCount: Int,
    ) : PeriodInstanceInterface

    fun unroll(segmentTree: SegmentTreeInterface) : List<PeriodInstanceInterface> {
        return unroll(segmentTree.workout, 0)
    }

    private fun unroll(set: SegmentInterface.Set, initialOffsetMs: Long) : List<PeriodInstance> {
        val result = mutableListOf<PeriodInstance>()
        var startOffsetMs: Long = initialOffsetMs
        for (rep in 0 until set.repeatCount) {
            for (child in set.children) {
                when (child) {
                    is SegmentInterface.Period -> {
                        val endOffsetMs =
                            startOffsetMs + TimeUnit.SECONDS.toMillis(child.totalDuration.toLong())
                        result.add(
                            PeriodInstance(
                            child.segmentId,
                            child.name,
                            child.totalDuration,
                            startOffsetMs,
                            endOffsetMs,
                            rep,
                            set.repeatCount
                        )
                        )
                        startOffsetMs = endOffsetMs
                    }
                    is SegmentInterface.Set -> {
                        result.addAll(unroll(child, startOffsetMs))
                        startOffsetMs = result.lastOrNull()?.endOffsetMs ?: 0
                    }
                    else ->
                        error("Unexpected child ${child.javaClass.name}")
                }
            }
        }
        return result
    }
}