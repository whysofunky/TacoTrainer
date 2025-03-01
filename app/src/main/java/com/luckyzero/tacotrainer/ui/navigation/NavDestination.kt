package com.luckyzero.tacotrainer.ui.navigation

import kotlinx.serialization.Serializable

sealed class NavDestination

@Serializable
data object WorkoutList : NavDestination()

@Serializable
data class WorkoutEdit(
    val workoutId: Long? = null,
    val newWorkout: Boolean = false,
) : NavDestination()

@Serializable
data class WorkoutExecute(
    val workoutId: Long
) : NavDestination()
