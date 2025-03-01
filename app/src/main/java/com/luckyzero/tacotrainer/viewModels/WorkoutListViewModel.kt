package com.luckyzero.tacotrainer.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.database.SegmentEntity
import com.luckyzero.tacotrainer.database.WorkoutEntity
import com.luckyzero.tacotrainer.models.PersistedWorkoutInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutListViewModel(dbAccess: DbAccess) : ViewModel() {

    companion object {
    }

    private val workoutDao = dbAccess.db.workoutDao()
    private val _listFlow = MutableStateFlow<List<PersistedWorkout>>(emptyList())
    val listFlow: StateFlow<List<PersistedWorkoutInterface>> = _listFlow

    init {
        viewModelScope.launch {
            loadWorkouts()
        }
    }

    fun createWorkout(name: String, totalDuration: Int) {
        viewModelScope.launch {
            val workout = WorkoutEntity(
                0,
                name,
                totalDuration,
                1
            )
            val workoutId = withContext(Dispatchers.IO) {
                workoutDao.insert(workout)
            }
            loadWorkouts()
        }
    }

    private suspend fun loadWorkouts() {
        val databaseList = withContext(Dispatchers.IO) {
            workoutDao.getAllWorkouts()
        }
        val uiModelList = databaseList.map {
            PersistedWorkout(
                it.id,
                it.name,
                it.totalDuration,
                it.repeatCount
            )
        }
        _listFlow.value = uiModelList
    }

    data class PersistedWorkout(
        override val id: Long,
        override val name: String,
        override val totalDuration: Int,
        override val repeatCount: Int,
    ) : PersistedWorkoutInterface
}