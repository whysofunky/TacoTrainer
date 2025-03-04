package com.luckyzero.tacotrainer.models

interface SegmentInterface {
    val segmentId: Long?
    val totalDuration: Int

    interface Set : SegmentInterface {
        val repeatCount: Int
        val children: List<SegmentInterface>
    }

    interface ChildSet : Set, SegmentInterface {
        override val segmentId: Long
        val parent: SegmentInterface
    }

    interface Period : SegmentInterface {
        override val segmentId: Long
        val name: String
        val duration: Int
        val parent: SegmentInterface

        val durationMs: Long get() = duration.toLong() * 1000
    }
}

