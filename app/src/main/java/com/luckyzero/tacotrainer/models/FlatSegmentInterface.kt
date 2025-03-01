package com.luckyzero.tacotrainer.models

interface FlatSegmentInterface {
    val id: Long?
    val parent: Set?
    val depth: Int

    interface Set : FlatSegmentInterface {
        val repeatCount: Int
    }

    interface Period : FlatSegmentInterface {
        override val id: Long
        val name: String
        val duration: Int
    }

    interface SetFooter : FlatSegmentInterface {
        override val id: Long?
        val set: Set
    }
}

