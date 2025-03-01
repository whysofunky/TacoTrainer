package com.luckyzero.tacotrainer.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Workout",
    indices = []
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    var name: String,
    var totalDuration: Int,
    var repeatCount: Int,
)