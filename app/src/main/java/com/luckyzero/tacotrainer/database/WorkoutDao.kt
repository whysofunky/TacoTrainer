package com.luckyzero.tacotrainer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM Workout")
    suspend fun getAllWorkouts() : List<WorkoutEntity>

    @Query("SELECT * FROM Workout WHERE id=(:id)")
    suspend fun lookupWorkout(id: Long): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutEntity: WorkoutEntity): Long

    @Update
    suspend fun update(workoutEntity: WorkoutEntity)

    @Delete
    suspend fun delete(workoutEntity: WorkoutEntity)

    @Query("DELETE FROM Workout WHERE id=(:id)")
    suspend fun delete(id: Long)
}