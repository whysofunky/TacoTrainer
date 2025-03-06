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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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
private const val INDENT_DEPTH = 12
private const val DIVIDER_HEIGHT = 4
private const val DEFAULT_ROW_HEIGHT = 48
private val indentColor = Color.Green




private data class SegmentListContext(
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
    when (flatSegmentItem) {
        is FlatSegmentInterface.Set ->
            WorkoutSetItem(flatSegmentItem, segmentListContext)
        is FlatSegmentInterface.Period ->
            WorkoutPeriodItem(flatSegmentItem, segmentListContext)
        is FlatSegmentInterface.SetFooter ->
            WorkoutSetFooterItem(flatSegmentItem, segmentListContext)
    }
}

@Composable
private fun WorkoutSetItem(set: FlatSegmentInterface.Set,
                           segmentListContext: SegmentListContext) {
    val indent = (set.depth + 1) * INDENT_DEPTH
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(segmentListContext.selectedItem.value == set.id) {
        if (segmentListContext.selectedItem.value == set.id) {
            focusRequester.requestFocus()
        }
    }
    Column {
        Box(modifier = Modifier.background(indentColor).fillMaxWidth().height(DIVIDER_HEIGHT.dp))
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(indent.dp)
                    .fillMaxHeight(1f)
                    .background(color = Color.Green)
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (segmentListContext.selectedItem.value == set.id) {
                EditableWorkoutSetItem(set, segmentListContext, focusRequester)
            } else {
                StaticWorkoutSetItem(set, segmentListContext)
            }
        }
    }
}

@Composable
private fun StaticWorkoutSetItem(set: FlatSegmentInterface.Set,
                                 segmentListContext: SegmentListContext) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(DEFAULT_ROW_HEIGHT.dp)
            .clickable { segmentListContext.selectedItem.value = set.id }
    ) {
        if (set.repeatCount == 1 && set.depth == 0) {
            Text(text = "Main set", fontSize = 20.sp)
        } else {
            Text(text = "Repeat: ", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = set.repeatCount.toString(),
                fontSize = 20.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(96.dp))
        }
    }
}

@Composable
private fun EditableWorkoutSetItem(set: FlatSegmentInterface.Set,
                                   segmentListContext: SegmentListContext,
                                   focusRequester: FocusRequester) {
    val repeatCount = rememberSaveable { mutableIntStateOf(set.repeatCount) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(DEFAULT_ROW_HEIGHT.dp)
                .clickable { segmentListContext.selectedItem.value = null }
        ) {
            Text(text = "Repeat: ", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            CountField(repeatCount,
                textStyle = TextStyle(fontSize = 20.sp),
                Modifier.width(96.dp).focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Row {
            Button(onClick = {
                segmentListContext.selectedItem.value = null
                if (set.id == 0L) {
                    segmentListContext.viewModel.updateWorkout(null, repeatCount.intValue)
                } else {
                    segmentListContext.viewModel.updateSet(set.id, repeatCount.intValue)
                }
            }) { Text(text = "Save") }
            Button(onClick = {
                segmentListContext.selectedItem.value = null
            }) {
                Text(text = "Discard")
            }
            if (set.id != 0L) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    segmentListContext.selectedItem.value = null
                    segmentListContext.viewModel.deleteSet(set.id)
                }) {
                    Text(text = "Delete")
                }
            }
        }
    }
}

@Composable
private fun WorkoutPeriodItem(period: FlatSegmentInterface.Period,
                              segmentListContext: SegmentListContext) {
    val indent = period.depth * INDENT_DEPTH
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(segmentListContext.selectedItem.value == period.id) {
        if (segmentListContext.selectedItem.value == period.id) {
            focusRequester.requestFocus()
        }
    }
    Column {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(indent.dp)
                    .fillMaxHeight(1f)
                    .background(color = Color.Green)
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (segmentListContext.selectedItem.value == period.id) {
                EditablePeriodItem(period, segmentListContext, focusRequester)
            } else {
                StaticPeriodItem(period, segmentListContext)
            }
        }
    }
}

@Composable
private fun StaticPeriodItem(period: FlatSegmentInterface.Period,
                             segmentListContext: SegmentListContext) {
    Column(modifier = Modifier.clickable { segmentListContext.selectedItem.value = period.id }) {
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
                               segmentListContext: SegmentListContext,
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
                segmentListContext.selectedItem.value = null
                segmentListContext.viewModel.updatePeriod(period.id, name.value, duration.intValue)
            }) { Text(text = "Save") }
            Button(onClick = {
                segmentListContext.selectedItem.value = null
            }) {
                Text(text = "Discard")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                segmentListContext.selectedItem.value = null
                segmentListContext.viewModel.deletePeriod(period.id)
            }) {
                Text(text = "Delete")
            }
        }
    }
}

@Composable
private fun WorkoutSetFooterItem(setFooter: FlatSegmentInterface.SetFooter,
                                 segmentListContext: SegmentListContext) {
    val indent = (setFooter.depth + 1) * INDENT_DEPTH
    Column {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(indent.dp)
                    .fillMaxHeight(1f)
                    .background(color = Color.Green)
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (setFooter.id == segmentListContext.selectedItem.value) {
                EditableWorkoutSetFooterItem(setFooter, segmentListContext)
            } else {
                StaticWorkoutSetFooterItem(setFooter, segmentListContext)
            }
        }
        Box(modifier = Modifier.background(indentColor).fillMaxWidth().height(DIVIDER_HEIGHT.dp))
    }
}

@Composable
private fun StaticWorkoutSetFooterItem(setFooter: FlatSegmentInterface.SetFooter,
                                       segmentListContext: SegmentListContext) {
    Box(modifier = Modifier.background(Color.White).fillMaxWidth().height(DIVIDER_HEIGHT.dp))
}


@Composable
private fun EditableWorkoutSetFooterItem(setFooter: FlatSegmentInterface.SetFooter,
                                         segmentListContext: SegmentListContext) {
    Row {
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = {
            val setIdFlow = segmentListContext.viewModel.createSet(setFooter.id)
            segmentListContext.coroutineScope.launch {
                setIdFlow.take(1).collect {
                    segmentListContext.selectedItem.value = it
                }
            }
        }, modifier = Modifier.padding(start = 8.dp)) {
            Text(text = "Add Set", fontSize = 16.sp)
        }
        Button(onClick = {
            val periodIdFlow = segmentListContext.viewModel.createPeriod(setFooter.id)
            segmentListContext.coroutineScope.launch {
                periodIdFlow.take(1).collect {
                    segmentListContext.selectedItem.value = it
                }
            }
        }, modifier = Modifier.padding(start = 8.dp)) {
            Text(text = "Add Period", fontSize = 16.sp)
        }
    }
}


