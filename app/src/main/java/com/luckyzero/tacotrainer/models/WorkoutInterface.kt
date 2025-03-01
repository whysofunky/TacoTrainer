package com.luckyzero.tacotrainer.models

interface WorkoutInterface {
    val id: Long?
    val name: String?
    val totalDuration: Int
    val repeatCount: Int
}

interface PersistedWorkoutInterface : WorkoutInterface {
    override val id: Long
    override val name: String
    override val repeatCount: Int
}