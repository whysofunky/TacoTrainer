package com.luckyzero.tacotrainer.ui.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.repositories.WorkoutTimer
import com.luckyzero.tacotrainer.ui.navigation.WorkoutExecute
import com.luckyzero.tacotrainer.ui.utils.UIUtils
import com.luckyzero.tacotrainer.viewModels.WorkoutExecuteViewModel
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
    primary = Color(0xFFC03030),
    secondary = Color(0xFFC05050),
    highlight = Color(0xFF600000),
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

private const val INITIAL_LAUNCH = "InitialLaunch"

// TODO: This will ultimately come from the current period
private val currentTheme = redTheme


private const val TAG = "WorkoutExecutePage"

@Composable
fun WorkoutExecutePage(args: WorkoutExecute,
                       navHostController: NavHostController,
                       modifier: Modifier
) {
    val dbAccess = DbAccess(LocalContext.current)
    val viewModel = viewModel { WorkoutExecuteViewModel(args.workoutId, dbAccess) }
    LaunchedEffect(INITIAL_LAUNCH) {
        viewModel.start()
    }
    Column(modifier = Modifier.fillMaxSize().background(color = currentTheme.background)) {
        WorkoutHeader(viewModel)
        PeriodName(viewModel)
        SetRepetition(viewModel)
        RemainDuration(viewModel)
        ButtonBar(viewModel)
    }


    // workout name
    // total duration remain / total duration
    // segment name
    // repetition / of repetition
    // remain duration
    // details ( HR range, watt range, gear range, etc.)
    // pause, end workout buttons
}


@Composable
fun WorkoutHeader(viewModel: WorkoutExecuteViewModel) {
    val workout by viewModel.workoutFlow.collectAsStateWithLifecycle(null)
    val totalDurationMs by viewModel.totalDurationMs.collectAsStateWithLifecycle(null)
    val elapsedMs by viewModel.totalElapsedTimeMsFlow.collectAsStateWithLifecycle(null)
    Column {
        Text(
            text = workout?.name ?: "",
            textAlign = TextAlign.Center,
            color = currentTheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
        Row(horizontalArrangement = Arrangement.Center) {
            val totalDuration = totalDurationMs?.let {
                UIUtils.formatDuration(UIUtils.millisToElapsedSeconds(it))
            }
            val elapsed = elapsedMs?.let {
                UIUtils.formatDuration(UIUtils.millisToElapsedSeconds(it))
            }
            Text(
                text = elapsed ?: "",
                color = currentTheme.secondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "of",
                color = currentTheme.secondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = totalDuration ?: "",
                color = currentTheme.secondary,
            )
        }
    }
}


@Composable
fun PeriodName(viewModel: WorkoutExecuteViewModel) {
    val instance by viewModel.currentPeriodFlow.collectAsStateWithLifecycle(null)
    Text(
        text = instance?.period?.name ?: ""
    )
}

@Composable
fun SetRepetition(viewModel: WorkoutExecuteViewModel) {
    val periodInstance by viewModel.currentPeriodFlow.collectAsStateWithLifecycle(null)
    periodInstance?.set?.let { setInstance ->
        Text(
            text = "${(setInstance.rep + 1)} of ${setInstance.set.repeatCount}"
        )
    }
}

@Composable
fun RemainDuration(viewModel: WorkoutExecuteViewModel) {
    val remainDurationValue by viewModel.periodRemainTimeMsFlow.collectAsStateWithLifecycle(null)
    val periodInstanceValue by viewModel.currentPeriodFlow.collectAsStateWithLifecycle(null)
    remainDurationValue?.let { remainDurationMs ->
        val remainDurationStr = UIUtils.formatDuration(UIUtils.millisToDurationSeconds(remainDurationMs))
        Text(
            text = remainDurationStr,
            color = currentTheme.primary,
            fontSize = 24.sp
        )
        periodInstanceValue?.period?.duration?.let { periodDuration ->
            val periodDurationMs = TimeUnit.SECONDS.toMillis(periodDuration.toLong())
            val periodCompleteness = (remainDurationMs.toFloat() / periodDurationMs.toFloat())
            CircularProgressIndicator(
                progress = { periodCompleteness }
            )
        }
    }
}

@Composable
fun ButtonBar(viewModel: WorkoutExecuteViewModel) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    Row {
        when (state) {
            WorkoutTimer.State.IDLE, WorkoutTimer.State.LOADED_WAITING -> {
                StartButton(viewModel)
            }
            WorkoutTimer.State.LOADING_READY, WorkoutTimer.State.RUNNING -> {
                PauseButton(viewModel)
            }
            WorkoutTimer.State.PAUSED -> {
                ResumeButton(viewModel)
            }
            WorkoutTimer.State.FINISHED -> {
                FinishedBanner()
            }
        }
    }
}


@Composable
fun StartButton(viewModel: WorkoutExecuteViewModel) {
    Button(onClick = {
        viewModel.start()
    }) {
        Text("State")
    }
}

@Composable
fun PauseButton(viewModel: WorkoutExecuteViewModel) {
    Button(onClick = {
        viewModel.pause()
    }) {
        Text("Pause")
    }
}

@Composable
fun ResumeButton(viewModel: WorkoutExecuteViewModel) {
    Button(onClick = {
        viewModel.resume()
    }) {
        Text("Resume")
    }
}


@Composable
fun FinishedBanner() {
    Text("Workout Finished")
}
