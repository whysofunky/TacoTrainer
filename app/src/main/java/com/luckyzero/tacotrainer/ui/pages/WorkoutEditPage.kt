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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.luckyzero.tacotrainer.R
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.FlatSegmentInterface
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.repositories.SegmentTreeLoader
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
private const val TREE_SPACER_WIDTH = 4
private const val TREE_INDICATOR_WIDTH = 6
private const val TREE_INDICATOR_HEIGHT = 6
private const val DEFAULT_ROW_HEIGHT = 48
private const val TREE_INDICATOR_CORNER_SIZE = 10
private val INDENT_COLOR = Color(0xFF2060C0)

private data class WorkoutEditContext(
    val viewModel: WorkoutEditViewModel,
    val selectedItem: MutableState<Long?>,
    val coroutineScope: CoroutineScope
)

@Composable
fun WorkoutEditPage(args: WorkoutEdit,
                    navHostController: NavHostController,
                    modifier: Modifier) {
    val dbAccess = DbAccess(LocalContext.current)
    val segmentTreeLoader = SegmentTreeLoader(dbAccess)
    val viewModel = viewModel { WorkoutEditViewModel(args.workoutId, segmentTreeLoader) }
    Column {
        PageHeading(viewModel)
        SegmentList(viewModel, navHostController)
    }
}

@Composable
private fun PageHeading(viewModel: WorkoutEditViewModel) {
    val workout = viewModel.workoutFlow.collectAsStateWithLifecycle().value
    if (workout != null) {
        // TODO: Make this use the same "selected item" mutable state, so that only one item
        // can be selected at a time.
        val editing = remember { mutableStateOf(false) }
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
                Text(text = stringResource(R.string.placeholder_workout_name),
                    color = Color.Blue
                )
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

@Composable
private fun SegmentList(viewModel: WorkoutEditViewModel, navController: NavHostController) {
    val flatSegments by viewModel.flatSegmentFlow.collectAsStateWithLifecycle(emptyList())
    val selectedItem = remember { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val workoutEditContext = WorkoutEditContext(viewModel, selectedItem, coroutineScope)
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
    LaunchedEffect(workoutEditContext.selectedItem.value == set.id) {
        if (workoutEditContext.selectedItem.value == set.id) {
            focusRequester.requestFocus()
        }
    }
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        TreeIndicator(set.depth + 1, roundedTop = true)
        Column {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(TREE_INDICATOR_HEIGHT.dp)
                .background(INDENT_COLOR)
            )
            Row {
                Spacer(modifier = Modifier.width(TREE_SPACER_WIDTH.dp))
                if (workoutEditContext.selectedItem.value == set.id) {
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
            .height(DEFAULT_ROW_HEIGHT.dp)
            .clickable { workoutEditContext.selectedItem.value = set.id }
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
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(DEFAULT_ROW_HEIGHT.dp)
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
            Button(onClick = {
                workoutEditContext.selectedItem.value = null
                if (set.id == 0L) {
                    workoutEditContext.viewModel.updateWorkout(null, repeatCount.intValue)
                } else {
                    workoutEditContext.viewModel.updateSet(set.id, repeatCount.intValue)
                }
            }) {
                Text(text = stringResource(R.string.button_save))
            }
            Button(onClick = {
                workoutEditContext.selectedItem.value = null
            }) {
                Text(text = stringResource(R.string.button_discard))
            }
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
    LaunchedEffect(workoutEditContext.selectedItem.value == period.id) {
        if (workoutEditContext.selectedItem.value == period.id) {
            focusRequester.requestFocus()
        }
    }
    Column {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            TreeIndicator(period.depth)
            Spacer(modifier = Modifier.width(TREE_SPACER_WIDTH.dp))
            if (workoutEditContext.selectedItem.value == period.id) {
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
    Column(modifier = Modifier.clickable { workoutEditContext.selectedItem.value = period.id }) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(DEFAULT_ROW_HEIGHT.dp)
        ) {
            Text(
                text = period.name,
                fontSize = 20.sp
            )
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
            modifier = Modifier.height(DEFAULT_ROW_HEIGHT.dp)
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
            Button(onClick = {
                workoutEditContext.selectedItem.value = null
                workoutEditContext.viewModel.updatePeriod(period.id, name.value, duration.intValue)
            }) { Text(text = stringResource(R.string.button_save)) }
            Button(onClick = {
                workoutEditContext.selectedItem.value = null
            }) {
                Text(text = stringResource(R.string.button_discard))
            }
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
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        TreeIndicator(setFooter.depth + 1, roundedBottom = true)
        Column {
            Row {
                Spacer(modifier = Modifier.width(TREE_SPACER_WIDTH.dp))
                if (setFooter.id == workoutEditContext.selectedItem.value) {
                    EditableWorkoutSetFooterItem(setFooter, workoutEditContext)
                } else {
                    StaticWorkoutSetFooterItem(setFooter, workoutEditContext)
                }
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(TREE_INDICATOR_HEIGHT.dp)
                .background(INDENT_COLOR)
            )
        }
    }
}

@Composable
private fun StaticWorkoutSetFooterItem(setFooter: FlatSegmentInterface.SetFooter,
                                       workoutEditContext: WorkoutEditContext) {
    Box(modifier = Modifier.background(Color.White).fillMaxWidth().height(TREE_INDICATOR_HEIGHT.dp))
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
                    workoutEditContext.selectedItem.value = it
                }
            }
        }, modifier = Modifier.padding(start = 8.dp)) {
            Text(text = stringResource(R.string.button_add_set), fontSize = 16.sp)
        }
        Button(onClick = {
            val periodIdFlow = workoutEditContext.viewModel.createPeriod(setFooter.id)
            workoutEditContext.coroutineScope.launch {
                periodIdFlow.take(1).collect {
                    workoutEditContext.selectedItem.value = it
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
                .width(TREE_INDICATOR_WIDTH.dp)
                .fillMaxHeight(1f)
                .background(INDENT_COLOR)
        )
        Spacer(modifier = Modifier.width(TREE_SPACER_WIDTH.dp))
    }
    Box(
        modifier = Modifier
            .width(TREE_INDICATOR_WIDTH.dp)
            .fillMaxHeight(1f)
            .clip(RoundedCornerShape(
                topStart = if (roundedTop) TREE_INDICATOR_CORNER_SIZE.dp else 0.dp,
                bottomStart = if (roundedBottom) TREE_INDICATOR_CORNER_SIZE.dp else 0.dp
            ))
            .background(INDENT_COLOR)
    )
}
