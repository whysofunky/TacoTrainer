package com.luckyzero.tacotrainer.models

interface SetInstanceInterface {
    val set: SegmentInterface.Set
    val rep: Int
}

interface PeriodInstanceInterface {
    val period: SegmentInterface.Period
    val set: SetInstanceInterface
    val startOffsetMs: Long
    val endOffsetMs: Long
}