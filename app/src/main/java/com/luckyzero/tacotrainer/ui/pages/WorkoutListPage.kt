package com.luckyzero.tacotrainer.ui.pages

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.luckyzero.tacotrainer.R
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.models.PersistedWorkoutInterface
import com.luckyzero.tacotrainer.ui.utils.UIUtils.formatDuration
import com.luckyzero.tacotrainer.ui.navigation.WorkoutEdit
import com.luckyzero.tacotrainer.ui.navigation.WorkoutExecute
import com.luckyzero.tacotrainer.viewModels.WorkoutListViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

private data class WorkoutListContext(
    val navController: NavHostController,
    val viewModel: WorkoutListViewModel
)

@Composable
fun WorkoutListPage(navController: NavHostController,
    viewModel: WorkoutListViewModel = hiltViewModel(),
    modifier: Modifier) {
    val listContext = WorkoutListContext(navController, viewModel)
    Column(modifier = modifier) {
        PageHeading()
        WorkoutList(listContext)
        Spacer(modifier = Modifier.weight(1f))
        Footer(listContext)
    }
}

@Composable
private fun PageHeading() {
    Text(
        text = "Workouts",
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    )
}

@Composable
private fun WorkoutList(listContext: WorkoutListContext) {
    val workoutList = listContext. viewModel.listFlow.collectAsStateWithLifecycle().value
    LazyColumn(modifier = Modifier.padding(12.dp)) {
        items(workoutList.size) {
            WorkoutItem(workoutList[it], listContext)
        }
    }
}

@Composable
private fun Footer(listContext: WorkoutListContext) {
    Row(modifier = Modifier.padding(12.dp)) {
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = {
            listContext.navController.navigate(WorkoutEdit(newWorkout = true))
        }) {
            Text(text = "New")
        }
    }
}

@Composable
private fun WorkoutItem(model: PersistedWorkoutInterface, listContext: WorkoutListContext) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row() {
            Text(text = model.name, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = formatDuration(model.totalDuration))
        }
        Row() {
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                listContext.navController.navigate(WorkoutExecute(model.id))
            }, modifier = Modifier.padding(start = 8.dp)) {
                Text(text = stringResource(R.string.button_run), fontSize = 16.sp)
            }
            Button(onClick = {
                listContext.navController.navigate(WorkoutEdit(workoutId = model.id))
            }, modifier = Modifier.padding(start = 8.dp)) {
                Text(text = stringResource(R.string.button_edit), fontSize = 16.sp)
            }
            Button(onClick = {
                listContext.viewModel.deleteWorkout(model.id)
//                Toast.makeText(context, "Not implemented", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.padding(start = 8.dp)) {
                Text(text = stringResource(R.string.button_delete), fontSize = 16.sp)
            }
        }
    }
}