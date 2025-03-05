package com.luckyzero.tacotrainer.repositories

import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.database.SegmentEntity
import com.luckyzero.tacotrainer.database.WorkoutEntity
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


interface SegmentTreeInterface {
    val workout: SegmentInterface.Workout

    suspend fun createSet(parentSet: SegmentInterface.Set) : SegmentInterface.ChildSet
    suspend fun createPeriod(parentSet: SegmentInterface.Set) : SegmentInterface.Period
    suspend fun updateWorkout(name: String?, repeatCount: Int?)
    suspend fun updateSet(set: SegmentInterface.Set, repeatCount: Int?)
    suspend fun updatePeriod(period: SegmentInterface.Period, name: String?, duration: Int?)
    suspend fun deleteSet(set: SegmentInterface.Set)
    suspend fun deletePeriod(period: SegmentInterface.Period)
}

class SegmentTreeLoader(dbAccess: DbAccess) {
    private val workoutDao by lazy { dbAccess.db.workoutDao() }
    private val segmentDao by lazy { dbAccess.db.segmentDao() }

    suspend fun createWorkout(name: String) : SegmentTreeInterface {
        val workoutEntity = WorkoutEntity(
            0,
            name,
            0,
            1
        )
        val workoutId = workoutDao.insert(workoutEntity)
        return loadWorkout(workoutId)
    }

    suspend fun loadWorkout(workoutId: Long) : SegmentTreeInterface {
        return workoutDao.lookupWorkout(workoutId)?.let { dbWorkout ->
            MutableWorkout(
                dbWorkout.id,
                dbWorkout.name,
                dbWorkout.repeatCount
            ).apply {
                setChildren(loadWorkoutChildren(this))
            }.let {
                SegmentTree(it)
            }
        } ?: throw IllegalStateException("No such workout $workoutId")
    }

    private inner class SegmentTree(
        val mutableWorkout: MutableWorkout,
    ) : SegmentTreeInterface {
        override val workout: SegmentInterface.Workout = mutableWorkout

        override suspend fun createSet(parentSet: SegmentInterface.Set) : SegmentInterface.ChildSet {
            val parent = parentSet as MutableParent
            val set = MutableChildSet(
                0,
                parent,
                1
            )
            parent.children.add(set)
            val id = createSetEntity(set)
            return set
        }

        override suspend fun createPeriod(parentSet: SegmentInterface.Set) : SegmentInterface.Period {
            val parent = parentSet as MutableParent
            val period = MutablePeriod(
                0,
                parent,
                "",
                0
            )
            parent.children.add(period)
            val id = createPeriodEntity(period)
            return period
        }

        override suspend fun updateWorkout(name: String?, repeatCount: Int?) {
            if (name != null) mutableWorkout.name = name
            if (repeatCount != null) mutableWorkout.repeatCount = repeatCount
            updateWorkoutEntity(mutableWorkout)
        }

        override suspend fun updateSet(set: SegmentInterface.Set, repeatCount: Int?) {
            set as MutableChildSet
            if (repeatCount != null) set.repeatCount = repeatCount
            updateSetEntity(set)
        }

        override suspend fun updatePeriod(period: SegmentInterface.Period, name: String?, duration: Int?) {
            period as MutablePeriod
            if (name != null) period.name = name
            if (duration != null) period.duration = duration
            updatePeriodEntity(period)
        }

        override suspend fun deleteSet(set: SegmentInterface.Set) {
            set as MutableChildSet
            val parent = set.parent
            parent.children.remove(set)
            deleteSegmentEntity(set)
        }

        override suspend fun deletePeriod(period: SegmentInterface.Period) {
            period as MutablePeriod
            val parent = period.parent
            parent.children.remove(period)
            deleteSegmentEntity(period)
        }

        private suspend fun createSetEntity(set: MutableChildSet): Long {
            val parent = set.parent
            val (workoutId, parentSegmentId) = parentIds(parent)
            val sequence = parent.children.indexOf(set)

            val segmentEntity =  SegmentEntity(
                0,
                parentSegmentId,
                workoutId,
                set.repeatCount,
                null,
                null,
                sequence
            )
            return withContext(Dispatchers.IO) {
                segmentDao.insert(segmentEntity)
            }
        }

        private suspend fun createPeriodEntity(period: MutablePeriod): Long {
            val parent = period.parent
            val (workoutId, parentSegmentId) = parentIds(parent)
            val sequence = parent.children.indexOf(period)

            val segmentEntity = SegmentEntity(
                0,
                parentSegmentId,
                workoutId,
                null,
                period.name,
                period.duration,
                sequence
            )
            return withContext(Dispatchers.IO) {
                val id = segmentDao.insert(segmentEntity)
                period.segmentId = id
                id
            }
        }

        private suspend fun updateWorkoutEntity(workout: MutableWorkout) {
            val workoutEntity = WorkoutEntity(
                workout.workoutId,
                workout.name,
                workout.totalDuration,
                workout.repeatCount
            )
            return withContext(Dispatchers.IO) {
                workoutDao.update(workoutEntity)
            }
        }

        private suspend fun updateSetEntity(set: MutableChildSet) {
            val parent = set.parent
            val (workoutId, parentSegmentId) = parentIds(parent)
            val sequence = parent.children.indexOf(set)

            val segmentEntity =  SegmentEntity(
                set.segmentId,
                parentSegmentId,
                workoutId,
                set.repeatCount,
                null,
                null,
                sequence
            )
            withContext(Dispatchers.IO) {
                segmentDao.update(segmentEntity)
            }
        }

        private suspend fun updatePeriodEntity(period: MutablePeriod) {
            val parent = period.parent
            val (workoutId, parentSegmentId) = parentIds(parent)
            val sequence = parent.children.indexOf(period)

            val segmentEntity = SegmentEntity(
                period.segmentId,
                parentSegmentId,
                workoutId,
                null,
                period.name,
                period.duration,
                sequence
            )
            withContext(Dispatchers.IO) {
                segmentDao.insert(segmentEntity)
            }
        }

        private suspend fun deleteSegmentEntity(segment: MutableChild) {
            withContext(Dispatchers.IO) {
                segmentDao.delete(segment.segmentId)
                segment.parent.children.forEachIndexed { index, segment ->
                    segmentDao.updateSequence(segment.segmentId, index)
                }
            }
        }

        private fun parentIds(parent: MutableParent) : Pair<Long?, Long?> {
            return Pair(
                (parent as? MutableWorkout)?.workoutId,
                (parent as? MutableChildSet)?.segmentId
            )
        }
    }

    private suspend fun loadWorkoutChildren(workout: MutableWorkout) : List<MutableChild> {
        val dbSegments = segmentDao.segmentsByWorkout(workout.workoutId)
        return loadChildren(workout, dbSegments)
    }

    private suspend fun loadSegmentChildren(set: MutableChildSet) : List<MutableChild> {
        val dbSegments = segmentDao.segmentsByParent(set.segmentId)
        return loadChildren(set, dbSegments)
    }

    private suspend fun loadChildren(parent: MutableParent, dbSegments: List<SegmentEntity>) : List<MutableChild> {
        return dbSegments.map { segment ->
            segment.repeatCount?.let { repeatCount ->
                MutableChildSet(
                    segment.id,
                    parent,
                    repeatCount,
                ).apply {
                    setChildren(loadSegmentChildren(this))
                }
            } ?: run {
                MutablePeriod(
                    segment.id,
                    parent,
                    segment.name ?: "",
                    segment.duration ?: 0,
                )
            }
        }
    }

    private interface MutableParent : SegmentInterface.Set {
        override val children : MutableList<MutableChild>
    }

    private interface MutableChild : SegmentInterface {
        override val segmentId: Long
        val parent: MutableParent
    }

    private class MutableWorkout(
        override val workoutId: Long,
        override var name: String,
        override var repeatCount: Int
    ) : SegmentInterface.Workout, MutableParent{
        override val segmentId: Long?
            get() = null
        override val children = mutableListOf<MutableChild>()
        override val totalDuration: Int
            get() = children.sumOf { it.totalDuration } * repeatCount
        fun setChildren(newChildren: List<MutableChild>) {
            children.clear()
            children.addAll(newChildren)
        }
    }

    private class MutableChildSet(
        override var segmentId: Long,
        override var parent: MutableParent,
        override var repeatCount: Int,
    ): SegmentInterface.ChildSet, MutableParent, MutableChild {
        override val children = mutableListOf<MutableChild>()
        override val totalDuration: Int
            get() = children.sumOf { it.totalDuration } * repeatCount
        fun setChildren(newChildren: List<MutableChild>) {
            children.clear()
            children.addAll(newChildren)
        }
    }

    private class MutablePeriod(
        override var segmentId: Long,
        override var parent: MutableParent,
        override var name: String,
        override var duration: Int,
    ) : SegmentInterface.Period, MutableChild {
        override val totalDuration: Int get() = duration
    }
}