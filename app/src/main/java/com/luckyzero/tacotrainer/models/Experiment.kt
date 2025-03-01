package com.luckyzero.tacotrainer.models

import com.luckyzero.tacotrainer.viewModels.WorkoutEditViewModel.MutableChild

interface Seg {
    val segmentId: Long?
    val totalDuration: Int

    interface RootSet : Seg {
        val children: List<Seg>
        val repeatCount: Int
    }

    interface Set : RootSet {
        override val segmentId: Long
        val parent: RootSet
    }

    interface Period : Seg {
        override val segmentId: Long
        val parent: RootSet
    }
}


interface FlatSeg {
    val model: Seg
    val segmentId: Long?

    interface Set : FlatSeg {
        override val model: Seg.RootSet
        override val segmentId: Long?
        val parent: Set?
    }

    interface Period : FlatSeg {
        override val model: Seg.Period
        override val segmentId: Long
        val parent: Set
    }

    interface SetFooter : FlatSeg {
        override val model: Seg.RootSet
        val parent: Set
    }
}
