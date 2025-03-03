package com.luckyzero.tacotrainer.models

interface SegmentInterface {
    val segmentId: Long?
    val totalDuration: Int

    interface RootSet : SegmentInterface {
        val repeatCount: Int
        val children: List<SegmentInterface>
    }

    interface Set : RootSet, SegmentInterface {
        override val segmentId: Long
        val parent: SegmentInterface
    }

    interface Period : SegmentInterface {
        override val segmentId: Long
        val name: String
        val duration: Int
        val parent: SegmentInterface
    }
}

