package com.luckyzero.tacotrainer.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luckyzero.tacotrainer.models.FlatSegmentInterface
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.repositories.SegmentTreeInterface
import com.luckyzero.tacotrainer.repositories.SegmentTreeLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorkoutEditViewModel(
    workoutId: Long?,
    private val segmentTreeLoader: SegmentTreeLoader,
) : ViewModel() {

    companion object {
        const val ROOT_SET_ID = 0L
        const val NEW_WORKOUT_NAME = "New Workout"
    }

    private lateinit var segmentTree: SegmentTreeInterface
    private val _workoutFlow = MutableStateFlow<WorkoutInterface?>(null)
    private val _flatSegmentFlow = MutableStateFlow<List<FlatSegmentInterface>>(emptyList())
    val workoutFlow: StateFlow<WorkoutInterface?> = _workoutFlow
    val flatSegmentFlow: StateFlow<List<FlatSegmentInterface>> = _flatSegmentFlow

    init {
        viewModelScope.launch {
            segmentTree = if (workoutId == null) {
                segmentTreeLoader.createWorkout(NEW_WORKOUT_NAME)
            } else {
                segmentTreeLoader.loadWorkout(workoutId)
            }
            publishFlatList()
        }
    }

    fun createSet(parentSetId: Long) : Flow<Long> {
        val parent = getParent(parentSetId)
        val flow = MutableSharedFlow<Long>()
        // TODO: This means we are waiting for the db operation to complete before publishing
        // To change this, the segmentTree would need to have its own lifecycle so that it could
        // return while still finishing the db operations.
        viewModelScope.launch {
            val newSet = segmentTree.createSet(parent)
            publishFlatList()
            flow.emit(newSet.segmentId)
        }
        return flow
    }

    fun createPeriod(parentSetId: Long) : Flow<Long> {
        val parent = getParent(parentSetId)
        val flow = MutableSharedFlow<Long>()
        // TODO: This means we are waiting for the db operation to complete before publishing
        viewModelScope.launch {
            val newPeriod = segmentTree.createSet(parent)
            publishFlatList()
            flow.emit(newPeriod.segmentId)
        }
        return flow
    }

    fun updateWorkout(name: String?, repeatCount: Int?) {
        // TODO: This means we are waiting for the db operation to complete before publishing
        viewModelScope.launch {
            segmentTree.updateWorkout(name, repeatCount)
        }
        publishFlatList()
    }

    fun updateSet(segmentId: Long, repeatCount: Int?) {
        val set = findSet(segmentId)
        viewModelScope.launch {
            segmentTree.updateSet(set, repeatCount)
        }
        publishFlatList()
    }

    fun updatePeriod(segmentId: Long, name: String?, duration: Int?) {
        val period = findPeriod(segmentId)
        viewModelScope.launch {
            segmentTree.updatePeriod(period, name, duration)
        }
        publishFlatList()
    }

    fun deleteSet(segmentId: Long) {
        val set = findSet(segmentId)
        viewModelScope.launch {
            segmentTree.deleteSet(set)
        }
        publishFlatList()
    }

    fun deletePeriod(segmentId: Long) {
        val period = findPeriod(segmentId)
        viewModelScope.launch {
            segmentTree.deletePeriod(period)
        }
        publishFlatList()
    }

    private fun getParent(parentSetId: Long) : SegmentInterface.Set {
        return if (parentSetId == ROOT_SET_ID) {
            segmentTree.workout
        } else {
            findSet(parentSetId) ?: run { throw IllegalStateException("No such set $parentSetId") }
        }
    }

    private fun findSet(segmentId: Long) : SegmentInterface.Set {
        when (val segment = findSegment(segmentId)) {
            null ->
                throw IllegalStateException("No such segment $segmentId")
            !is SegmentInterface.Set ->
                throw IllegalStateException("Segment $segmentId is not a set")
            else ->
                return segment
        }
    }

    private fun findPeriod(segmentId: Long) : SegmentInterface.Period {
        when (val segment = findSegment(segmentId)) {
            null ->
                throw IllegalStateException("No such segment $segmentId")
            !is SegmentInterface.Period ->
                throw IllegalStateException("Segment $segmentId is not a period")
            else ->
                return segment
        }
    }

    private fun findSegment(segmentId: Long) : SegmentInterface? {
        val stack = ArrayDeque<SegmentInterface>()
        stack.addAll(segmentTree.workout.children)
        while (stack.isNotEmpty()) {
            val segment = stack.removeLast()
            if (segment.segmentId == segmentId) {
                return segment
            }
            if (segment is SegmentInterface.Set) {
                stack.addAll(segment.children)
            }
        }
        return null
    }

    private fun publishFlatList() {
        _workoutFlow.value = ImmutableWorkout(segmentTree.workout)
        _flatSegmentFlow.value = SegmentFlattener.flatten(segmentTree.workout)
    }

    private class ImmutableWorkout(
        override val id: Long,
        override val name: String,
        override val repeatCount: Int,
        override val totalDuration: Int,
    ): WorkoutInterface {
        constructor(workout: SegmentInterface.Workout) : this(
            workout.workoutId,
            workout.name,
            workout.repeatCount,
            workout.totalDuration
        )
    }
}