package com.luckyzero.tacotrainer.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.FlatSegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.ui.navigation.WorkoutEdit
import com.luckyzero.tacotrainer.ui.utils.UIUtils
import com.luckyzero.tacotrainer.viewModels.WorkoutEditViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "WorkoutEditScreen"
private const val INDENT_DEPTH = 8
private const val DIVIDER_HEIGHT = 4
private val indentColor = Color.Green

@Composable
fun WorkoutEditPage(args: WorkoutEdit,
                    navHostController: NavHostController,
                    modifier: Modifier) {
    val dbAccess = DbAccess(LocalContext.current)
    val viewModel = viewModel { WorkoutEditViewModel(args.workoutId, dbAccess) }
    Column {
        PageHeading(viewModel)
        SegmentList(viewModel, navHostController)
    }
}

@Composable
private fun PageHeading(viewModel: WorkoutEditViewModel) {
    val workout = viewModel.workoutFlow.collectAsStateWithLifecycle().value
    workout?.let {
        val editing = remember { mutableStateOf(workout.id == null) }
        if (editing.value) {
            EditablePageHeading(workout, updateWorkout = {
                viewModel.updateWorkout(it, null)
                editing.value = false
            })
        } else {
            StaticPageHeading(workout, onClick = {
                editing.value = true
            })
        }
    }
}

@Composable
private fun StaticPageHeading(workout: WorkoutInterface, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
        .clickable { onClick() }) {
        Text(
            text = workout.name ?: "",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = UIUtils.formatDuration(workout.totalDuration),
            fontSize = 20.sp
        )
    }
}

@Composable
private fun EditablePageHeading(
    workout: WorkoutInterface,
    updateWorkout: (name: String) -> Unit
) {
    var workoutName by rememberSaveable { mutableStateOf(workout.name ?: "") }
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)) {
        TextField(
            value = workoutName,
            singleLine = true,
            textStyle = TextStyle(fontSize = 20.sp),
            placeholder = {
                Text(text = "Workout name", color = Color.Blue)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    updateWorkout(workoutName)
                }
            ),
            onValueChange = { v -> workoutName = v },
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = UIUtils.formatDuration(workout.totalDuration),
            fontSize = 20.sp
        )
    }
}

private data class SegmentListContext(
    val viewModel: WorkoutEditViewModel,
    val selectedItem: MutableState<Long?>,
    val coroutineScope: CoroutineScope
)


@Composable
private fun SegmentList(viewModel: WorkoutEditViewModel, navController: NavHostController) {
    val flatSegments by viewModel.flatSegmentFlow.collectAsStateWithLifecycle(emptyList())
    val selectedItem = remember { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val segmentListContext = SegmentListContext(viewModel, selectedItem, coroutineScope)
    LazyColumn(modifier = Modifier.padding(12.dp)) {
        items(flatSegments.size) {
            SegmentItem(flatSegments[it], segmentListContext)
        }
    }
}

@Composable
private fun SegmentItem(flatSegmentItem: FlatSegmentInterface,
                        segmentListContext: SegmentListContext) {
    val indent = (flatSegmentItem.depth + 1) * INDENT_DEPTH
    Row(modifier = Modifier.background(color = indentColor)) {
        Box(modifier = Modifier.padding(start = indent.dp).background(color = Color.White)) {
            when (flatSegmentItem) {
                is FlatSegmentInterface.UniveralSet ->
                    WorkoutSetItem(flatSegmentItem, segmentListContext)
                is FlatSegmentInterface.Period ->
                    WorkoutPeriodItem(flatSegmentItem, segmentListContext)
                is FlatSegmentInterface.SetFooter ->
                    WorkoutSetFooterItem(flatSegmentItem, segmentListContext)
            }
        }
    }
}

@Composable
private fun WorkoutSetItem(set: FlatSegmentInterface.UniveralSet,
                           segmentListContext: SegmentListContext) {
    Column {
        Box(modifier = Modifier.background(indentColor).fillMaxWidth().height(DIVIDER_HEIGHT.dp))
        if (segmentListContext.selectedItem.value == set.id) {
            EditableWorkoutSetItem(set, segmentListContext)
        } else {
            StaticWorkoutSetItem(set, segmentListContext)
        }
    }
}

@Composable
private fun EditableWorkoutSetItem(set: FlatSegmentInterface.UniveralSet,
                                   segmentListContext: SegmentListContext) {
    Row(modifier = Modifier
        .clickable { segmentListContext.selectedItem.value = null }
    ) {
        Text(
            text = "Repeat: ${set.repeatCount}",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StaticWorkoutSetItem(set: FlatSegmentInterface.UniveralSet,
                                 segmentListContext: SegmentListContext) {
    Row(modifier = Modifier
        .clickable { segmentListContext.selectedItem.value = set.id }
    ) {
        Text(
            text = "Repeat: ${set.repeatCount}",
            fontSize = 20.sp
        )
    }
}

@Composable
private fun WorkoutPeriodItem(period: FlatSegmentInterface.Period,
                              segmentListContext: SegmentListContext) {
    if (segmentListContext.selectedItem.value == period.id) {
        EditablePeriodItem(period, segmentListContext)
    } else {
        StaticPeriodItem(period, segmentListContext)
    }
}

@Composable
private fun StaticPeriodItem(period: FlatSegmentInterface.Period,
                             segmentListContext: SegmentListContext) {
    Column(modifier = Modifier.clickable { segmentListContext.selectedItem.value = period.id }) {
        Row {
            Text(
                text = period.name,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = UIUtils.formatDuration(period.duration),
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun EditablePeriodItem(period: FlatSegmentInterface.Period,
                               segmentListContext: SegmentListContext) {
    var name by rememberSaveable { mutableStateOf(period.name) }
    var duration by rememberSaveable { mutableIntStateOf(period.duration) }
    Column {
        Row {
            TextField(
                value = name,
                singleLine = true,
                textStyle = TextStyle(fontSize = 20.sp),
                placeholder = { Text(text = "name", color = Color.Blue) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {}
                ),
                onValueChange = { v -> name = v },
            )
            Spacer(modifier = Modifier.weight(1f))
            TextField(
                // TODO: Better duration entry UI, should break hours:minutes:seconds
                value = duration.toString(),
                onValueChange = { v -> duration = v.toIntOrNull() ?: 0 },
                textStyle = TextStyle(fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Row {
            Button(onClick = {
                segmentListContext.selectedItem.value = null
                segmentListContext.viewModel.updatePeriod(period.id, name, duration)
            }) { Text(text = "Save") }
            Button(onClick = { segmentListContext.selectedItem.value = null }) {
                Text(text = "Discard")
            }
        }
    }
}

@Composable
private fun WorkoutSetFooterItem(setFooter: FlatSegmentInterface.SetFooter,
                                 segmentListContext: SegmentListContext) {
    Column {
        Row {
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                segmentListContext.coroutineScope.launch {
                    val newSetId = segmentListContext.viewModel.createSet(setFooter.id)
                    // TODO: Listen to the flow
                    // segmentListContext.selectedItem.value = newSetId
                }
            }, modifier = Modifier.padding(start = 8.dp)) {
                Text(text = "Add Set", fontSize = 16.sp)
            }
            Button(onClick = {
                segmentListContext.coroutineScope.launch {
                    val newPeriodId = segmentListContext.viewModel.createPeriod(setFooter.id)
                    // TODO: Listen to the flow
                    // segmentListContext.selectedItem.value = newPeriodId
                }
            }, modifier = Modifier.padding(start = 8.dp)) {
                Text(text = "Add Period", fontSize = 16.sp)
            }
        }
        Box(modifier = Modifier.background(indentColor).fillMaxWidth().height(DIVIDER_HEIGHT.dp))
    }
}


