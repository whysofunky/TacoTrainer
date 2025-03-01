package com.luckyzero.tacotrainer.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.database.SegmentEntity
import com.luckyzero.tacotrainer.database.WorkoutEntity
import com.luckyzero.tacotrainer.models.FlatSegmentInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_PERIOD_NAME = "Enter name"

class WorkoutEditViewModel(
    private var workoutId: Long?,
    private val dbAccess: DbAccess
) : ViewModel() {

    private val segmentDao by lazy { dbAccess.db.segmentDao() }
    private val workoutDao by lazy { dbAccess.db.workoutDao() }
    private var workout: MutableWorkout? = null

    private val _workoutFlow = MutableStateFlow<MutableWorkout?>(null)
    private val _flatSegmentFlow = MutableStateFlow<List<FlatSegmentInterface>>(emptyList())
    val workoutFlow: StateFlow<WorkoutInterface?> = _workoutFlow
    val flatSegmentFlow: StateFlow<List<FlatSegmentInterface>> = _flatSegmentFlow

    init {
        viewModelScope.launch {
            workoutId?.let {
                workout = loadWorkout(it)
            } ?: run {
                workout = createWorkout()
            }
            publishFlatList()
        }
    }

    fun createPeriod(parentSetId: Long?) : Flow<Long> {
        val parent = getParent(parentSetId)
        val period = MutablePeriod(0, parent, DEFAULT_PERIOD_NAME, 0)
        parent.children.add(period)
        return flow {
            emit(createSegmentEntity(period))
        }
    }

    fun createSet(parentSetId: Long?) : Flow<Long> {
        val parent = getParent(parentSetId)
        val set = MutableSet(0, parent, 1)
        parent.children.add(set)
        return flow {
            emit(createSegmentEntity(set))
        }
    }

    fun updateWorkout(name: String?, repeatCount: Int?) {
        val w = workout ?: run { throw IllegalStateException("Workout uninitialized") }
        if (name != null) w.name = name
        if (repeatCount != null) w.repeatCount = repeatCount
        // TODO: update db
        publishFlatList()
    }

    fun updatePeriod(segmentId: Long, name: String?, duration: Int?) {
        val segment = findSegment(segmentId)
        (segment as? MutablePeriod) ?: run {
            throw IllegalStateException("segment $segmentId is not a period")
        }
        if (name != null) segment.name = name
        if (duration != null) segment.duration = duration
        // TODO: update db
        publishFlatList()
    }

    fun updateSet(segmentId: Long, repeatCount: Int?) {
        val segment = findSegment(segmentId)
        (segment as? MutableSet) ?: run {
            throw IllegalStateException("segment $segmentId is not a set")
        }
        if (repeatCount != null) segment.repeatCount = repeatCount

        // TODO: update db
        publishFlatList()
    }

    private fun deletePeriod(segmentId: Long) {
        val segment = findSegment(segmentId)
        (segment as? MutablePeriod) ?: run {
            throw IllegalStateException("segment $segmentId is not a period")
        }
        val parent = segment.parent
        parent.children.remove(segment)

        // TODO: update db (all children need to be resequenced)
        publishFlatList()
    }

    private fun deleteSet(segmentId: Long) {
        val segment = findSegment(segmentId)
        (segment as? MutableSet) ?: run {
            throw IllegalStateException("segment $segmentId is not a set")
        }
        val parent = segment.parent
        parent.children.remove(segment)

        // TODO: update db (all children need to be resequenced)
        publishFlatList()
    }

    private fun getParent(parentSetId: Long?) : MutableParent {
        return if (parentSetId == null) {
            workout ?: run { throw IllegalStateException("Workout uninitialized" )}
        } else {
            (findSegment(parentSetId) as? MutableParent) ?: run {
                throw IllegalStateException("segment $parentSetId is not a Set")
            }
        }
    }

    private suspend fun createSegmentEntity(child: MutableChild): Long {
        val parent = child.parent
        val workoutId = (parent as? MutableWorkout)?.id
        val segmentId = (parent as? MutableSet)?.segmentId
        val sequence = parent.children.indexOf(child)

        val segmentEntity = when (child) {
            is MutablePeriod -> {
                SegmentEntity(
                    0,
                    segmentId,
                    workoutId,
                    null,
                    child.name,
                    child.duration,
                    sequence
                )
            }
            is MutableSet -> {
                SegmentEntity(
                    0,
                    segmentId,
                    workoutId,
                    child.repeatCount,
                    null,
                    null,
                    sequence
                )
            }
            else -> {
                throw IllegalStateException("Unexpected type ${child.javaClass.name}")
            }
        }
        return withContext(Dispatchers.IO) {
            segmentDao.insert(segmentEntity)
        }
    }

    private fun findSegment(segmentId: Long): MutableChild? {
        val stack = ArrayDeque<MutableChild>()
        workout?.let { stack.addAll(it.children) }

        while (stack.isNotEmpty()) {
            val segment = stack.removeLast()
            if (segment.segmentId == segmentId) {
                return segment
            }
            if (segment is MutableSet) {
                stack.addAll(segment.children)
            }
        }
        return null
    }

    private fun publishFlatList() {
        _workoutFlow.value = workout
        _flatSegmentFlow.value = SegmentFlattener.flatten(workout)
    }

    private suspend fun createWorkout() : MutableWorkout {
        val workoutEntity = WorkoutEntity(
            0,
            "New Workout",
            0,
            1
        )
        val workoutId = workoutDao.insert(workoutEntity)
        return loadWorkout(workoutId)
    }

    private suspend fun loadWorkout(workoutId: Long) : MutableWorkout {
        return workoutDao.lookupWorkout(workoutId)?.let { dbWorkout ->
            MutableWorkout(
                dbWorkout.id,
                dbWorkout.name,
                dbWorkout.repeatCount
            ).apply {
                setChildren(loadWorkoutChildren(this))
            }
        } ?: throw IllegalStateException("No such workout $workoutId")
    }

    private suspend fun loadWorkoutChildren(workout: MutableWorkout) : List<MutableChild> {
        val dbSegments = segmentDao.segmentsByWorkout(workout.id)
        return loadChildren(workout, dbSegments)
    }

    private suspend fun loadSegmentChildren(set: MutableSet) : List<MutableChild> {
        val dbSegments = segmentDao.segmentsByParent(set.segmentId)
        return loadChildren(set, dbSegments)
    }

    private suspend fun loadChildren(parent: MutableParent, dbSegments: List<SegmentEntity>) : List<MutableChild> {
        return dbSegments.map { segment ->
            segment.repeatCount?.let { repeatCount ->
                MutableSet(
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

    private interface MutableParent : SegmentInterface.Parent {
        override val children : MutableList<MutableChild>
    }

    private interface MutableChild : SegmentInterface.Child {
        override val parent: MutableParent
    }

    private class MutableWorkout(
        override val id: Long,
        override var name: String?,
        override var repeatCount: Int
    ) : WorkoutInterface, MutableParent, SegmentInterface.RootSet {
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

    private class MutableSet(
        override val segmentId: Long,
        override var parent: MutableParent,
        override var repeatCount: Int,
    ): SegmentInterface.Set, MutableParent, MutableChild {
        override val children = mutableListOf<MutableChild>()
        override val totalDuration: Int
            get() = children.sumOf { it.totalDuration } * repeatCount
        fun setChildren(newChildren: List<MutableChild>) {
            children.clear()
            children.addAll(newChildren)
        }
    }

    private class MutablePeriod(
        override val segmentId: Long,
        override var parent: MutableParent,
        override var name: String,
        override var duration: Int,
    ) : SegmentInterface.Period, MutableChild {
        override val totalDuration: Int get() = duration
    }
}