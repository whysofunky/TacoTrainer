package com.luckyzero.tacotrainer.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.repositories.SegmentTreeLoader
import com.luckyzero.tacotrainer.ui.navigation.WorkoutExecute
import com.luckyzero.tacotrainer.ui.utils.UIUtils
import com.luckyzero.tacotrainer.viewModels.WorkoutEditViewModel
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

@Composable
fun WorkoutExecutePage(args: WorkoutExecute,
                       navHostController: NavHostController,
                       modifier: Modifier
) {
    val dbAccess = DbAccess(LocalContext.current)
    val segmentTreeLoader = SegmentTreeLoader(dbAccess)
    val viewModel = viewModel { WorkoutExecuteViewModel(args.workoutId, segmentTreeLoader) }
    LaunchedEffect(INITIAL_LAUNCH) {
        viewModel.start()
    }
    Column {
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
    val workout by viewModel.workoutFlow.collectAsStateWithLifecycle()
    val totalDurationMs by viewModel.totalDurationMs.collectAsStateWithLifecycle(null)
    val elapsedMs by viewModel.totalElapsedTimeMsFlow.collectAsStateWithLifecycle(null)
    Column {
        Text(
            text = workout?.name ?: ""
        )
        Row {
            val totalDuration = totalDurationMs?.let {
                UIUtils.formatDuration(TimeUnit.MILLISECONDS.toSeconds(it).toInt())
            }
            val elapsed = elapsedMs?.let {
                UIUtils.formatDuration(TimeUnit.MILLISECONDS.toSeconds(it).toInt())
            }
            Text(
                text = elapsed ?: ""
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = totalDuration ?: ""
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
    val remainDurationMs by viewModel.periodRemainTimeMsFlow.collectAsStateWithLifecycle(null)
    remainDurationMs?.let {
        val remainDuration = UIUtils.formatDuration(TimeUnit.MILLISECONDS.toSeconds(it).toInt())
        Text(
            text = remainDuration
        )
    }
}

@Composable
fun ButtonBar(viewModel: WorkoutExecuteViewModel) {
    Row {
        Button(onClick = {
            viewModel.pause()
        }) {
            Text("Pause")
        }
        Button(onClick = {
            viewModel.resume()
        }) {
            Text("Resume")
        }
    }
}

