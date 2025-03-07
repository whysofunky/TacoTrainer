package com.luckyzero.tacotrainer.models

interface PeriodInstanceInterface {
    val segmentId: Long
    val name: String
    val duration: Int
    val startOffsetMs: Long
    val endOffsetMs: Long
    val repetition: Int
    val setRepeatCount: Int
}