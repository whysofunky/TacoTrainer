package com.luckyzero.tacotrainer.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Segment",
    foreignKeys = [
        ForeignKey(
            entity = SegmentEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("parentId"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("workoutId"),
            onDelete = ForeignKey.CASCADE
        )],
    indices = [
        Index(value = arrayOf("parentId", "sequence")),
        Index(value = arrayOf("workoutId", "sequence"))
    ])
data class SegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val parentId: Long?,
    val workoutId: Long?,
    var repeatCount: Int?,
    var name: String?,
    var duration: Int?,
    var sequence: Int,
)