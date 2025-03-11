package com.luckyzero.tacotrainer.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.luckyzero.tacotrainer.R
import com.luckyzero.tacotrainer.models.FlatSegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.ui.navigation.WorkoutEdit
import com.luckyzero.tacotrainer.ui.utils.UIUtils
import com.luckyzero.tacotrainer.ui.widgets.CountField
import com.luckyzero.tacotrainer.ui.widgets.DurationField
import com.luckyzero.tacotrainer.ui.widgets.NameField
import com.luckyzero.tacotrainer.viewModels.WorkoutEditViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

private const val TAG = "WorkoutEditScreen"
private const val TREE_SPACER_WIDTH_DP = 4
private const val TREE_INDICATOR_WIDTH_DP = 6
private const val TREE_INDICATOR_HEIGHT_DP = 6
private const val TREE_INDICATOR_CORNER_SIZE_DP = 10
private const val DEFAULT_ROW_HEIGHT_DP = 48
private const val BUTTON_SPACE_DP = 8

private sealed class ItemId {
    data object Workout : ItemId()
    data class Set(val id: Long) : ItemId()
    data class Period(val id: Long) : ItemId()
}

private data class WorkoutEditContext(
    val viewModel: WorkoutEditViewModel,
    val selectedItem: MutableState<ItemId?>,
    val coroutineScope: CoroutineScope
)

@Composable
fun WorkoutEditPage(args: WorkoutEdit,
                    navHostController: NavHostController,
                    modifier: Modifier) {
    val viewModel: WorkoutEditViewModel =
        hiltViewModel<WorkoutEditViewModel, WorkoutEditViewModel.Factory> { factory ->
            factory.create(args.workoutId)
        }
    val selectedItem = remember { mutableStateOf<ItemId?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val workoutEditContext = WorkoutEditContext(viewModel, selectedItem, coroutineScope)

    Column {
        PageHeading(workoutEditContext)
        SegmentList(workoutEditContext)
    }
}

@Composable
private fun PageHeading(workoutEditContext: WorkoutEditContext) {
    val workout = workoutEditContext.viewModel.workoutFlow.collectAsStateWithLifecycle().value
    if (workout != null) {
        if (workoutEditContext.selectedItem.value == ItemId.Workout) {
            EditablePageHeading(
                workout,
                updateWorkout = {
                    workoutEditContext.viewModel.updateWorkout(it, null)
                }, dismiss = {
                    workoutEditContext.selectedItem.value = null
                }
            )
        } else {
            StaticPageHeading(
                workout,
                onClick = {
                    workoutEditContext.selectedItem.value = ItemId.Workout
                }
            )
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
    updateWorkout: (name: String) -> Unit,
    dismiss: () -> Unit

) {
    var workoutName by rememberSaveable { mutableStateOf(workout.name ?: "") }
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            TextField(
                value = workoutName,
                singleLine = true,
                textStyle = TextStyle(fontSize = 20.sp),
                placeholder = {
                    Text(text = stringResource(R.string.placeholder_workout_name))
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        updateWorkout(workoutName)
                        dismiss()
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
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            SaveButton(onClick = {
                updateWorkout(workoutName)
                dismiss()
            })
            Spacer(modifier = Modifier.width(BUTTON_SPACE_DP.dp))
            CancelButton(onClick = {
                dismiss()
            })
        }
    }
}

@Composable
private fun SegmentList(workoutEditContext: WorkoutEditContext) {
    val flatSegments by
    workoutEditContext.viewModel.flatSegmentFlow.collectAsStateWithLifecycle(emptyList())
    LazyColumn(modifier = Modifier.padding(12.dp)) {
        items(flatSegments.size) {
            SegmentItem(flatSegments[it], workoutEditContext)
        }
    }
}

@Composable
private fun SegmentItem(flatSegmentItem: FlatSegmentInterface,
                        workoutEditContext: WorkoutEditContext) {
    when (flatSegmentItem) {
        is FlatSegmentInterface.Set ->
            WorkoutSetItem(flatSegmentItem, workoutEditContext)
        is FlatSegmentInterface.Period ->
            WorkoutPeriodItem(flatSegmentItem, workoutEditContext)
        is FlatSegmentInterface.SetFooter ->
            WorkoutSetFooterItem(flatSegmentItem, workoutEditContext)
    }
}

@Composable
private fun WorkoutSetItem(set: FlatSegmentInterface.Set,
                           workoutEditContext: WorkoutEditContext) {
    val focusRequester = remember { FocusRequester() }
    val itemId = ItemId.Set(set.id)
    LaunchedEffect(workoutEditContext.selectedItem.value == itemId) {
        if (workoutEditContext.selectedItem.value == itemId) {
            focusRequester.requestFocus()
        }
    }
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        TreeIndicator(set.depth + 1, roundedTop = true)
        Column {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(TREE_INDICATOR_HEIGHT_DP.dp)
                .background(color = MaterialTheme.colorScheme.primary)
            )
            Row {
                Spacer(modifier = Modifier.width(TREE_SPACER_WIDTH_DP.dp))
                if (workoutEditContext.selectedItem.value == itemId) {
                    EditableWorkoutSetItem(set, workoutEditContext, focusRequester)
                } else {
                    StaticWorkoutSetItem(set, workoutEditContext)
                }
            }
        }
    }
}

@Composable
private fun StaticWorkoutSetItem(set: FlatSegmentInterface.Set,
                                 workoutEditContext: WorkoutEditContext) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(DEFAULT_ROW_HEIGHT_DP.dp)
            .clickable { workoutEditContext.selectedItem.value = ItemId.Set(set.id) }
    ) {
        if (set.repeatCount == 1 && set.depth == 0) {
            Text(text = stringResource(R.string.heading_main_set), fontSize = 20.sp)
        } else {
            Text(
                text = stringResource(R.string.heading_repeat_count, set.repeatCount),
                fontSize = 20.sp,
            )
        }
    }
}

@Composable
private fun EditableWorkoutSetItem(set: FlatSegmentInterface.Set,
                                   workoutEditContext: WorkoutEditContext,
                                   focusRequester: FocusRequester) {
    val repeatCount = rememberSaveable { mutableIntStateOf(set.repeatCount) }

    Column {
        if (set.depth == 0) {
            Text(text = stringResource(R.string.heading_main_set), fontSize = 20.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(DEFAULT_ROW_HEIGHT_DP.dp)
                .clickable { workoutEditContext.selectedItem.value = null }
        ) {
            Text(text = stringResource(R.string.heading_repeat), fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            CountField(repeatCount,
                textStyle = TextStyle(fontSize = 20.sp),
                Modifier.width(96.dp).focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Row {
            SaveButton(onClick = {
                workoutEditContext.selectedItem.value = null
                if (set.id == 0L) {
                    workoutEditContext.viewModel.updateWorkout(null, repeatCount.intValue)
                } else {
                    workoutEditContext.viewModel.updateSet(set.id, repeatCount.intValue)
                }
            })
            Spacer(modifier = Modifier.width(BUTTON_SPACE_DP.dp))
            CancelButton(onClick = {
                workoutEditContext.selectedItem.value = null
            })
            if (set.id != 0L) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    workoutEditContext.selectedItem.value = null
                    workoutEditContext.viewModel.deleteSet(set.id)
                }) {
                    Text(text = stringResource(R.string.button_delete))
                }
            }
        }
    }
}

@Composable
private fun WorkoutPeriodItem(period: FlatSegmentInterface.Period,
                              workoutEditContext: WorkoutEditContext) {
    val focusRequester = remember { FocusRequester() }
    val itemId = ItemId.Period(period.id)
    LaunchedEffect(workoutEditContext.selectedItem.value == itemId) {
        if (workoutEditContext.selectedItem.value == itemId) {
            focusRequester.requestFocus()
        }
    }
    Column {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            TreeIndicator(period.depth)
            Spacer(modifier = Modifier.width(TREE_SPACER_WIDTH_DP.dp))
            if (workoutEditContext.selectedItem.value == itemId) {
                EditablePeriodItem(period, workoutEditContext, focusRequester)
            } else {
                StaticPeriodItem(period, workoutEditContext)
            }
        }
    }
}

@Composable
private fun StaticPeriodItem(period: FlatSegmentInterface.Period,
                             workoutEditContext: WorkoutEditContext) {
    Column(
        modifier = Modifier
            .height(DEFAULT_ROW_HEIGHT_DP.dp)
            .clickable { workoutEditContext.selectedItem.value = ItemId.Period(period.id) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(DEFAULT_ROW_HEIGHT_DP.dp)
        ) {
            if (period.name.isBlank()) {
                Text(
                    text = stringResource(R.string.placeholder_period_name),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                Text(
                    text = period.name,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f).widthIn(min=12.dp))
            Text(
                text = UIUtils.formatDuration(period.duration),
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun EditablePeriodItem(period: FlatSegmentInterface.Period,
                               workoutEditContext: WorkoutEditContext,
                               focusRequester: FocusRequester) {
    val name = rememberSaveable { mutableStateOf(period.name) }
    val duration = rememberSaveable { mutableIntStateOf(period.duration) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(DEFAULT_ROW_HEIGHT_DP.dp)
        ) {
            NameField(
                name,
                textStyle = TextStyle(fontSize = 20.sp),
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
            Spacer(modifier = Modifier.widthIn(min=12.dp))
            DurationField(
                duration = duration,
                textStyle = TextStyle(fontSize = 20.sp),
                modifier = Modifier.width(96.dp),
            )
        }
        Row {
            SaveButton(onClick = {
                workoutEditContext.selectedItem.value = null
                workoutEditContext.viewModel.updatePeriod(period.id, name.value, duration.intValue)
            })
            Spacer(modifier = Modifier.width(BUTTON_SPACE_DP.dp))
            CancelButton(onClick = {
                workoutEditContext.selectedItem.value = null
            })
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                workoutEditContext.selectedItem.value = null
                workoutEditContext.viewModel.deletePeriod(period.id)
            }) {
                Text(text = stringResource(R.string.button_delete))
            }
        }
    }
}

@Composable
private fun WorkoutSetFooterItem(setFooter: FlatSegmentInterface.SetFooter,
                                 workoutEditContext: WorkoutEditContext) {
    val itemId = ItemId.Set(setFooter.id)
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        TreeIndicator(setFooter.depth + 1, roundedBottom = true)
        Column {
            Row {
                Spacer(modifier = Modifier.width(TREE_SPACER_WIDTH_DP.dp))
                if (workoutEditContext.selectedItem.value == itemId) {
                    EditableWorkoutSetFooterItem(setFooter, workoutEditContext)
                } else {
                    StaticWorkoutSetFooterItem(setFooter, workoutEditContext)
                }
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(TREE_INDICATOR_HEIGHT_DP.dp)
                .background(color = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun SaveButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = stringResource(R.string.button_save))
    }
}

@Composable
fun CancelButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = stringResource(R.string.button_cancel))
    }
}

@Composable
private fun StaticWorkoutSetFooterItem(setFooter: FlatSegmentInterface.SetFooter,
                                       workoutEditContext: WorkoutEditContext) {
    Box(modifier = Modifier.fillMaxWidth().height(TREE_INDICATOR_HEIGHT_DP.dp))
}

@Composable
private fun EditableWorkoutSetFooterItem(setFooter: FlatSegmentInterface.SetFooter,
                                         workoutEditContext: WorkoutEditContext) {
    Row {
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = {
            val setIdFlow = workoutEditContext.viewModel.createSet(setFooter.id)
            workoutEditContext.coroutineScope.launch {
                setIdFlow.take(1).collect {
                    workoutEditContext.selectedItem.value = ItemId.Set(setFooter.id)
                }
            }
        }, modifier = Modifier.padding(start = 8.dp)) {
            Text(text = stringResource(R.string.button_add_set), fontSize = 16.sp)
        }
        Button(onClick = {
            val periodIdFlow = workoutEditContext.viewModel.createPeriod(setFooter.id)
            workoutEditContext.coroutineScope.launch {
                periodIdFlow.take(1).collect {
                    workoutEditContext.selectedItem.value = ItemId.Set(setFooter.id)
                }
            }
        }, modifier = Modifier.padding(start = 8.dp)) {
            Text(text = stringResource(R.string.button_add_period), fontSize = 16.sp)
        }
    }
}

@Composable
private fun TreeIndicator(
    depth: Int,
    roundedTop: Boolean = false,
    roundedBottom: Boolean = false
) {
    for (i in 1 until depth) {
        Box(
            modifier = Modifier
                .width(TREE_INDICATOR_WIDTH_DP.dp)
                .fillMaxHeight(1f)
                .background(color = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(TREE_SPACER_WIDTH_DP.dp))
    }
    Box(
        modifier = Modifier
            .width(TREE_INDICATOR_WIDTH_DP.dp)
            .fillMaxHeight(1f)
            .clip(RoundedCornerShape(
                topStart = if (roundedTop) TREE_INDICATOR_CORNER_SIZE_DP.dp else 0.dp,
                bottomStart = if (roundedBottom) TREE_INDICATOR_CORNER_SIZE_DP.dp else 0.dp
            ))
            .background(color = MaterialTheme.colorScheme.primary)
    )
}
