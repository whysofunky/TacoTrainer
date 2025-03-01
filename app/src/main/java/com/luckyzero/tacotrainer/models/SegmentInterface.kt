package com.luckyzero.tacotrainer.models

interface SegmentInterface {
    val totalDuration: Int

    interface Parent : SegmentInterface {
        val children: List<Child>
    }

    interface Child : SegmentInterface {
        val segmentId: Long
        val parent: Parent
    }

    interface RootSet : Parent {
        val repeatCount: Int
        val segmentId: Long?
    }

    interface Set : RootSet, Child {
        override val segmentId: Long
    }

    interface Period : Child {
        val name: String
        val duration: Int
    }
}

