package com.luckyzero.tacotrainer.models

interface FlatSegmentInterface {
    val id: Long?
    val parent: UniveralSet?
    val depth: Int

    /*
    interface RootSet : FlatSegmentInterface {
        val repeatCount: Int
    }

    interface Set : FlatSegmentInterface {
        override val id: Long
        val repeatCount: Int
    }
*/
    interface UniveralSet : FlatSegmentInterface {
        val repeatCount: Int
    }

    interface Period : FlatSegmentInterface {
        override val id: Long
        val name: String
        val duration: Int
    }

    interface SetFooter : FlatSegmentInterface {
        override val id: Long?
        val set: UniveralSet
    }
}

