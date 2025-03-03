package com.luckyzero.tacotrainer.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SegmentDao {
    @Query("SELECT * FROM Segment")
    suspend fun getAllSegments() : List<SegmentEntity>

    @Query("SELECT * FROM Segment WHERE id=(:id) ORDER BY sequence")
    suspend fun lookupSegment(id: Long): SegmentEntity?

    @Query("SELECT * FROM Segment WHERE parentId=(:parentId) ORDER BY sequence")
    suspend fun segmentsByParent(parentId: Long): List<SegmentEntity>

    @Query("SELECT * FROM Segment WHERE workoutId=(:workoutId)")
    suspend fun segmentsByWorkout(workoutId: Long): List<SegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segmentEntity: SegmentEntity): Long

    @Update
    suspend fun update(segmentEntity: SegmentEntity)

    @Query("UPDATE Segment SET sequence=(:sequence) WHERE id=(:segmentId)")
    suspend fun updateSequence(segmentId: Long, sequence: Int)

    @Delete
    suspend fun delete(segmentEntity: SegmentEntity)

    @Query("DELETE FROM Segment WHERE id=(:id)")
    suspend fun delete(id: Long)
}