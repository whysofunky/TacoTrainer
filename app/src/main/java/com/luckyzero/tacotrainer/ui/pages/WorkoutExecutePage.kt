package com.luckyzero.tacotrainer.ui.pages

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.luckyzero.tacotrainer.R
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.repositories.WorkoutTimer
import com.luckyzero.tacotrainer.ui.navigation.WorkoutExecute
import com.luckyzero.tacotrainer.ui.utils.UIUtils
import com.luckyzero.tacotrainer.ui.utils.visibility
import com.luckyzero.tacotrainer.viewModels.WorkoutExecuteViewModel
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local

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
    val viewModel: WorkoutExecuteViewModel
)

@Composable
fun WorkoutExecutePage(args: WorkoutExecute,
                       navHostController: NavHostController,
                       modifier: Modifier
) {
    val context = LocalContext.current
    //val workManager = WorkManager.getInstance(context)
    //val workRequest = OneTimeWorkRequestBuilder<TimerWorker>()
    //    .setInputData(Data.Builder().putLong("id", args.workoutId).build())


    // TODO: Put the timer implementation into the Worker, and then get info from it with
    // an api like this:
    // workManager.getWorkInfosFlow()
    val dbAccess = DbAccess(LocalContext.current)
    val viewModel = viewModel { WorkoutExecuteViewModel(args.workoutId, dbAccess) }
    LaunchedEffect(EFFECT_INITIAL_LAUNCH) {
        viewModel.start()
    }
    val executeContext = WorkoutExecuteContext(navHostController, viewModel)
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
    val workout by
        executeContext.viewModel.workoutFlow.collectAsStateWithLifecycle(null)
    val totalDurationMs by
        executeContext.viewModel.totalDurationMs.collectAsStateWithLifecycle(null)
    val elapsedMs by
        executeContext.viewModel.totalElapsedTimeMsFlow.collectAsStateWithLifecycle(null)
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
            val totalDurationStr = UIUtils.formatDuration(UIUtils.millisToElapsedSeconds(
                totalDurationMs ?: 0L
            ))
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
    val instance by
        executeContext.viewModel.currentPeriodFlow.collectAsStateWithLifecycle(null)
    Text(
        text = instance?.period?.name ?: "",
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = currentTheme.primary,
        modifier = Modifier.padding(12.dp),
    )
}

@Composable
private fun PeriodState(executeContext: WorkoutExecuteContext) {
    val remainDurationValue by
        executeContext.viewModel.periodRemainTimeMsFlow.collectAsStateWithLifecycle(null)
    val periodInstanceValue by
    executeContext.viewModel.currentPeriodFlow.collectAsStateWithLifecycle(null)

    val remainDurationMs = remainDurationValue ?: 0L
    val period = periodInstanceValue?.period
    val setInstance = periodInstanceValue?.set
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
            val repStr = setInstance?.let {
                stringResource(
                    R.string.set_rep_and_count,
                    setInstance.rep,
                    setInstance.set.repeatCount,
                    )
            } ?: ""
            Text(
                text = repStr,
                fontSize = 24.sp,
                color = currentTheme.secondary,
                modifier = Modifier.visibility(setInstance != null)
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

}

@Composable
private fun ButtonBar(executeContext: WorkoutExecuteContext) {
    val state by executeContext.viewModel.stateFlow.collectAsStateWithLifecycle()
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(76.dp)
            .fillMaxWidth()
    ) {
        when (state) {
            WorkoutTimer.State.IDLE, WorkoutTimer.State.LOADED_WAITING -> {
                StartButton(executeContext)
                Spacer(modifier = Modifier.width(8.dp))
                EndButton(executeContext)
            }
            WorkoutTimer.State.LOADING_READY, WorkoutTimer.State.RUNNING -> {
                PauseButton(executeContext)
                Spacer(modifier = Modifier.width(8.dp))
                EndButton(executeContext)
            }
            WorkoutTimer.State.PAUSED -> {
                ResumeButton(executeContext)
                Spacer(modifier = Modifier.width(8.dp))
                EndButton(executeContext)
            }
            WorkoutTimer.State.FINISHED -> {
                FinishedBanner()
            }
        }
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
            fontSize=20.sp,
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
            fontSize=20.sp,
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
            executeContext.viewModel.pause()
            executeContext.navHostController.popBackStack()
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
