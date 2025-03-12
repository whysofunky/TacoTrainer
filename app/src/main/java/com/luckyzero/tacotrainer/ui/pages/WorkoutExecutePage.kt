package com.luckyzero.tacotrainer.ui.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.luckyzero.tacotrainer.R
import com.luckyzero.tacotrainer.models.WorkoutInterface
import com.luckyzero.tacotrainer.ui.navigation.WorkoutExecute
import com.luckyzero.tacotrainer.ui.utils.UIUtils
import com.luckyzero.tacotrainer.ui.utils.visibility
import com.luckyzero.tacotrainer.viewModels.WorkoutExecuteViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private data class ColorTheme(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val contrast: Color,
    val colorless: Color,
    val colorlessInverse: Color,
)

private val redTheme = ColorTheme(
    background = Color(0xFFD08080),
    primary = Color(0xFFB02020),
    secondary = Color(0xFFC04040),
    highlight = Color(0xFF600808),
    contrast = Color(0xFFC0C000),
    colorless = Color(0xFF101010),
    colorlessInverse = Color(0xFFF0F0F0)
)

private val greenTheme = ColorTheme(
    background = Color(0xFF80D080),
    primary = Color(0xFF30C030),
    secondary = Color(0xFF50C050),
    highlight = Color(0xFF006000),
    contrast = Color(0xFFC0C000),
    colorless = Color(0xFF101010),
    colorlessInverse = Color(0xFFF0F0F0)
)


// TODO: This will ultimately come from the current period
private val currentTheme = redTheme

private const val TAG = "WorkoutExecutePage"

private const val EFFECT_INITIAL_LAUNCH = "InitialLaunch"

private data class WorkoutExecuteContext(
    val navHostController: NavHostController,
    val viewModel: WorkoutExecuteViewModel,
) {
    val workout get() = viewModel.workoutFlow
    val state get() = viewModel.stateFlow
    val totalElapsedTimeMs get() = viewModel.totalElapsedTimeMsFlow
    val periodRemainTimeMs get() = viewModel.periodRemainTimeMsFlow
    val currentPeriod get() = viewModel.currentPeriodFlow
    val nextPeriod get() = viewModel.nextPeriodFlow
}

@Composable
fun WorkoutExecutePage(args: WorkoutExecute,
                       navHostController: NavHostController,
                       modifier: Modifier
) {
    val viewModel: WorkoutExecuteViewModel = hiltViewModel()
    val executeContext = WorkoutExecuteContext(navHostController, viewModel)
    var autoStart = remember { true }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(EFFECT_INITIAL_LAUNCH) {
        if (viewModel.workoutFlow.value?.id != args.workoutId) {
            // TODO: If we have an existing workout and this one doesn't match, present a
            // dialog asking of we should continue the existing one or start the new one.
            viewModel.loadWorkout(workoutId = args.workoutId)
        }
        coroutineScope.launch {
            viewModel.stateFlow.filterNotNull().takeWhile { autoStart }.collect {
                if (it == WorkoutExecuteViewModel.State.READY) {
                    viewModel.start()
                    autoStart = false
                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(color = currentTheme.background)) {
        WorkoutHeader(executeContext)
        Spacer(modifier = Modifier.weight(1f))
        CurrentPeriodInfo(executeContext)
        Spacer(modifier = Modifier.weight(3f))
        ButtonBar(executeContext)
    }
}

@Composable
private fun WorkoutHeader(executeContext: WorkoutExecuteContext) {
    val workout by executeContext.workout.collectAsStateWithLifecycle(null)
    val elapsedMs by executeContext.totalElapsedTimeMs.collectAsStateWithLifecycle(null)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(12.dp)
    ) {
        Text(
            text = workout?.name ?: "",
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = currentTheme.primary,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )
        Row(horizontalArrangement = Arrangement.Center) {
            val totalDuration = workout?.totalDuration
            val totalDurationStr = UIUtils.formatDuration(totalDuration ?: 0)
            val elapsedTimeStr = UIUtils.formatDuration(
                UIUtils.millisToElapsedSeconds(elapsedMs ?: 0L)
            )
            Text(
                text = stringResource(
                    R.string.workout_elapsed_and_duration,
                    elapsedTimeStr,
                    totalDurationStr
                ),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                color = currentTheme.secondary,
            )
        }
    }
}

@Composable
private fun CurrentPeriodInfo(executeContext: WorkoutExecuteContext) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PeriodName(executeContext)
        PeriodState(executeContext)
        PeriodNotes(executeContext)
    }
}

@Composable
private fun PeriodName(executeContext: WorkoutExecuteContext) {
    val instance by executeContext.currentPeriod.collectAsStateWithLifecycle(null)
    Text(
        text = instance?.name ?: "",
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = currentTheme.primary,
        modifier = Modifier.padding(12.dp),
    )
}

@Composable
private fun PeriodState(executeContext: WorkoutExecuteContext) {
    val remainDurationValue by executeContext
        .periodRemainTimeMs.collectAsStateWithLifecycle(null)
    val period by executeContext.currentPeriod.collectAsStateWithLifecycle(null)
    val remainDurationMs = remainDurationValue ?: 0L
    val completeness = period?.duration?.let {
        (remainDurationMs.toFloat() / (TimeUnit.SECONDS.toMillis(it.toLong()).toFloat()))
    } ?: 1f

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = UIUtils.formatDuration(UIUtils.millisToDurationSeconds(remainDurationMs)),
                color = currentTheme.highlight,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp),
            )
            val repStr = period?.let {
                stringResource(
                    R.string.set_rep_and_count,
                    it.repetition + 1,
                    it.setRepeatCount,
                    )
            } ?: ""
            Text(
                text = repStr,
                fontSize = 24.sp,
                color = currentTheme.secondary,
                modifier = Modifier.visibility(period != null)
            )
        }
        CircularProgressIndicator(
            progress = { completeness },
            strokeWidth = 12.dp,
            trackColor = currentTheme.secondary,
            color = currentTheme.highlight,
            modifier = Modifier.width(250.dp).height(250.dp)
        )
    }
}

@Composable
private fun PeriodNotes(executeContext: WorkoutExecuteContext) {
    Spacer(modifier = Modifier.height(24.dp))
    Row {
        Text(
            text = "Target HR",
            textAlign = TextAlign.End,
            fontSize = 24.sp,
            color = currentTheme.secondary,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "120 - 130",
            textAlign = TextAlign.Start,
            fontSize = 24.sp,
            color = currentTheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
    Row {
        Text(
            text = "Target Pace",
            textAlign = TextAlign.End,
            fontSize = 24.sp,
            color = currentTheme.secondary,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "8:00",
            textAlign = TextAlign.Start,
            fontSize = 24.sp,
            color = currentTheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ButtonBar(executeContext: WorkoutExecuteContext) {
    val state by executeContext
        .state.collectAsStateWithLifecycle(WorkoutExecuteViewModel.State.IDLE)
    when (state) {
        WorkoutExecuteViewModel.State.IDLE -> {
            // This should be a transient state
        }
        WorkoutExecuteViewModel.State.LOADING, WorkoutExecuteViewModel.State.READY -> {
            ReadyButtonBar(executeContext)
        }
        WorkoutExecuteViewModel.State.RUNNING -> {
            RunningButtonBar(executeContext)
        }
        WorkoutExecuteViewModel.State.PAUSED -> {
            PausedButtonBar(executeContext)
        }
        WorkoutExecuteViewModel.State.FINISHED -> {
            FinishedButtonBar(executeContext)
        }
    }
}

@Composable
private fun ReadyButtonBar(executeContext: WorkoutExecuteContext) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(96.dp).fillMaxWidth()
    ) {
        StartButton(executeContext)
        Spacer(modifier = Modifier.width(8.dp))
        EndButton(executeContext)
    }
}

@Composable
private fun RunningButtonBar(executeContext: WorkoutExecuteContext) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(96.dp).fillMaxWidth()
    ) {
        PauseButton(executeContext)
        Spacer(modifier = Modifier.width(8.dp))
        EndButton(executeContext)
    }
}

@Composable
private fun PausedButtonBar(executeContext: WorkoutExecuteContext) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(96.dp).fillMaxWidth()
    ) {
        ResumeButton(executeContext)
        Spacer(modifier = Modifier.width(8.dp))
        EndButton(executeContext)
    }
}

@Composable
private fun FinishedButtonBar(executeContext: WorkoutExecuteContext) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.height(96.dp).fillMaxWidth()
    ) {
        RestartButton(executeContext)
        FinishedBanner()
    }
}

@Composable
private fun StartButton(executeContext: WorkoutExecuteContext) {
    Button(
        colors = ButtonColors(containerColor = currentTheme.colorless,
            contentColor = currentTheme.contrast,
            disabledContainerColor = currentTheme.colorlessInverse,
            disabledContentColor = currentTheme.colorless,
            ),
        onClick = {
            executeContext.viewModel.start()
        }
    ) {
        Text(
            text = stringResource(R.string.button_start),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            )
    }
}

@Composable
private fun PauseButton(executeContext: WorkoutExecuteContext) {
    Button(
        colors = ButtonColors(containerColor = currentTheme.colorless,
            contentColor = currentTheme.contrast,
            disabledContainerColor = currentTheme.colorlessInverse,
            disabledContentColor = currentTheme.colorless,
        ),
        onClick = {
            executeContext.viewModel.pause()
        }
    ) {
        Text(
            text = stringResource(R.string.button_pause),
            fontSize=20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResumeButton(executeContext: WorkoutExecuteContext) {
    Button(
        colors = ButtonColors(containerColor = currentTheme.colorless,
            contentColor = currentTheme.contrast,
            disabledContainerColor = currentTheme.colorlessInverse,
            disabledContentColor = currentTheme.colorless,
        ),
        onClick = {
            executeContext.viewModel.resume()
        }
    ) {
        Text(
            text = stringResource(R.string.button_resume),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RestartButton(executeContext: WorkoutExecuteContext) {
    Button(
        colors = ButtonColors(containerColor = currentTheme.colorless,
            contentColor = currentTheme.contrast,
            disabledContainerColor = currentTheme.colorlessInverse,
            disabledContentColor = currentTheme.colorless,
        ),
        onClick = {
            executeContext.viewModel.restart()
        }
    ) {
        Text(
            text = stringResource(R.string.button_restart),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EndButton(executeContext: WorkoutExecuteContext) {
    Button(
        colors = ButtonColors(containerColor = currentTheme.colorless,
            contentColor = currentTheme.contrast,
            disabledContainerColor = currentTheme.colorlessInverse,
            disabledContentColor = currentTheme.colorless,
        ),
        onClick = {
            executeContext.viewModel.stop()
        }
    ) {
        Text(
            text = stringResource(R.string.button_end_workout),
            fontSize=20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FinishedBanner() {
    Text(text = stringResource(R.string.workout_finished),
        color = currentTheme.colorless,
        fontSize=20.sp,
        fontWeight = FontWeight.Bold
    )
}
