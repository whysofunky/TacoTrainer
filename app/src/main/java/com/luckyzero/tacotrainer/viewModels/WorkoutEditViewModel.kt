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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutEditViewModel(
    private var workoutId: Long?,
    private val dbAccess: DbAccess
) : ViewModel() {

    private val segmentDao by lazy { dbAccess.db.segmentDao() }
    private val workoutDao by lazy { dbAccess.db.workoutDao() }
    private var workout: MutableWorkout? = null

    private val _workoutFlow = MutableStateFlow<WorkoutInterface?>(null)
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

    fun createSet(parentSetId: Long?) : Flow<Long> {
        val parent = getParent(parentSetId)
        val set = MutableSet(0, parent, 1)
        parent.children.add(set)
        val flow = MutableSharedFlow<Long>()
        viewModelScope.launch {
            val id = createSetEntity(set)
            publishFlatList()
            flow.emit(id)
        }
        return flow
    }

    fun createPeriod(parentSetId: Long?) : Flow<Long> {
        val parent = getParent(parentSetId)
        val period = MutablePeriod(0, parent, "", 0)
        parent.children.add(period)
        val flow = MutableSharedFlow<Long>()
        viewModelScope.launch {
            val id = createPeriodEntity(period)
            publishFlatList()
            flow.emit(id)
        }
        return flow
    }

    fun updateWorkout(name: String?, repeatCount: Int?) {
        val w = workout ?: run { throw IllegalStateException("Workout uninitialized") }
        if (name != null) w.name = name
        if (repeatCount != null) w.repeatCount = repeatCount
        viewModelScope.launch {
            updateWorkoutEntity(w)
        }
        publishFlatList()
    }

    fun updateSet(segmentId: Long, repeatCount: Int?) {
        val set = findSegment(segmentId)
        (set as? MutableSet) ?: run {
            throw IllegalStateException("segment $segmentId is not a set")
        }
        if (repeatCount != null) set.repeatCount = repeatCount
        viewModelScope.launch {
            updateSetEntity(set)
        }
        publishFlatList()
    }

    fun updatePeriod(segmentId: Long, name: String?, duration: Int?) {
        val period = findSegment(segmentId)
        (period as? MutablePeriod) ?: run {
            throw IllegalStateException("segment $segmentId is not a period")
        }
        if (name != null) period.name = name
        if (duration != null) period.duration = duration
        viewModelScope.launch {
            updatePeriodEntity(period)
        }
        publishFlatList()
    }

    fun deleteSet(segmentId: Long) {
        val segment = findSegment(segmentId)
        (segment as? MutableSet) ?: run {
            throw IllegalStateException("segment $segmentId is not a set")
        }
        val parent = segment.parent
        parent.children.remove(segment)
        viewModelScope.launch {
            deleteSegmentEntity(segment)
        }
        publishFlatList()
    }

    fun deletePeriod(segmentId: Long) {
        val segment = findSegment(segmentId)
        (segment as? MutablePeriod) ?: run {
            throw IllegalStateException("segment $segmentId is not a period")
        }
        val parent = segment.parent
        parent.children.remove(segment)
        viewModelScope.launch {
            deleteSegmentEntity(segment)
        }
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
        _workoutFlow.value = workout?.toImmutable()
        _flatSegmentFlow.value = SegmentFlattener.flatten(workout)
    }

    // DB code

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

    private suspend fun createSetEntity(set: MutableSet): Long {
        val parent = set.parent
        val workoutId = (parent as? MutableWorkout)?.id
        val parentSegmentId = (parent as? MutableSet)?.segmentId
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
        val workoutId = (parent as? MutableWorkout)?.id
        val parentSegmentId = (parent as? MutableSet)?.segmentId
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
            workout.id,
            workout.name,
            workout.totalDuration,
            workout.repeatCount
        )
        return withContext(Dispatchers.IO) {
            workoutDao.update(workoutEntity)
        }
    }

    private suspend fun updateSetEntity(set: MutableSet) {
        val parent = set.parent
        val workoutId = (parent as? MutableWorkout)?.id
        val parentSegmentId = (parent as? MutableSet)?.segmentId
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
        val workoutId = (parent as? MutableWorkout)?.id
        val parentSegmentId = (parent as? MutableSet)?.segmentId
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

    private class ImmutableWorkout(
        override val id: Long,
        override val name: String,
        override val repeatCount: Int,
        override val totalDuration: Int,
    ): WorkoutInterface

    private interface MutableParent : SegmentInterface {
        val children : MutableList<MutableChild>
    }

    private interface MutableChild : SegmentInterface {
        override val segmentId: Long
        val parent: MutableParent
    }

    private class MutableWorkout(
        val id: Long,
        var name: String,
        override var repeatCount: Int
    ) : MutableParent, SegmentInterface.RootSet {
        override val segmentId: Long?
            get() = null
        override val children = mutableListOf<MutableChild>()
        override val totalDuration: Int
            get() = children.sumOf { it.totalDuration } * repeatCount
        fun setChildren(newChildren: List<MutableChild>) {
            children.clear()
            children.addAll(newChildren)
        }

        fun toImmutable() = ImmutableWorkout(id, name, repeatCount, totalDuration)
    }

    private class MutableSet(
        override var segmentId: Long,
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
        override var segmentId: Long,
        override var parent: MutableParent,
        override var name: String,
        override var duration: Int,
    ) : SegmentInterface.Period, MutableChild {
        override val totalDuration: Int get() = duration
    }
}