package com.luckyzero.tacotrainer.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.database.SegmentEntity
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class WorkoutExecuteViewModel(
    private var workoutId: Long,
    private val dbAccess: DbAccess
) : ViewModel() {
    private val segmentDao by lazy { dbAccess.db.segmentDao() }
    private val workoutDao by lazy { dbAccess.db.workoutDao() }

    private val _workoutFlow = MutableSharedFlow<WorkoutInterface>()
    private val _setFlow = MutableSharedFlow<SegmentInterface.Set>()
    val workoutFlow: SharedFlow<WorkoutInterface> = _workoutFlow
    val setFlow: SharedFlow<SegmentInterface.Set> = _setFlow

    init {
        viewModelScope.launch {
            val workout = loadWorkout(workoutId)
            _workoutFlow.emit(workout)
            _setFlow.emit(workout)
        }
    }

    private suspend fun loadWorkout(workoutId: Long) : WorkoutModel {
        return workoutDao.lookupWorkout(workoutId)?.let { dbWorkout ->
            WorkoutModel(
                dbWorkout.id,
                dbWorkout.name,
                dbWorkout.repeatCount
            ).apply {
                setChildren(loadWorkoutChildren(this))
            }
        } ?: throw IllegalStateException("No such workout $workoutId")
    }

    private suspend fun loadWorkoutChildren(workout: WorkoutModel) : List<ChildModel> {
        val dbSegments = segmentDao.segmentsByWorkout(workout.id)
        return loadChildren(workout, dbSegments)
    }

    private suspend fun loadSegmentChildren(set: ChildSetModel) : List<ChildModel> {
        val dbSegments = segmentDao.segmentsByParent(set.segmentId)
        return loadChildren(set, dbSegments)
    }

    private suspend fun loadChildren(parent: ParentModel, dbSegments: List<SegmentEntity>) : List<ChildModel> {
        return dbSegments.map { segment ->
            segment.repeatCount?.let { repeatCount ->
                ChildSetModel(
                    segment.id,
                    parent,
                    repeatCount,
                ).apply {
                    setChildren(loadSegmentChildren(this))
                }
            } ?: run {
                PeriodModel(
                    segment.id,
                    parent,
                    segment.name ?: "",
                    segment.duration ?: 0,
                )
            }
        }
    }

    private interface ParentModel : SegmentInterface {
        val children : MutableList<ChildModel>
    }

    private interface ChildModel : SegmentInterface {
        override val segmentId: Long
        val parent: ParentModel
    }

    private class WorkoutModel(
        override val id: Long,
        override var name: String,
        override var repeatCount: Int
    ) : WorkoutInterface, ParentModel,
        SegmentInterface.Set {
        override val segmentId: Long?
            get() = null
        override val children = mutableListOf<ChildModel>()
        override val totalDuration: Int
            get() = children.sumOf { it.totalDuration } * repeatCount
        fun setChildren(newChildren: List<ChildModel>) {
            children.clear()
            children.addAll(newChildren)
        }
    }

    private class ChildSetModel(
        override var segmentId: Long,
        override var parent: ParentModel,
        override var repeatCount: Int,
    ): SegmentInterface.ChildSet, ChildModel, ParentModel {
        override val children = mutableListOf<ChildModel>()
        override val totalDuration: Int
            get() = children.sumOf { it.totalDuration } * repeatCount
        fun setChildren(newChildren: List<ChildModel>) {
            children.clear()
            children.addAll(newChildren)
        }
    }

    private class PeriodModel(
        override var segmentId: Long,
        override var parent: ParentModel,
        override var name: String,
        override var duration: Int,
    ) : SegmentInterface.Period, ChildModel {
        override val totalDuration: Int get() = duration
    }
}