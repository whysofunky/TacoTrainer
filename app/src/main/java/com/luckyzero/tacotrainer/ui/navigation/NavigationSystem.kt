package com.luckyzero.tacotrainer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.luckyzero.tacotrainer.ui.pages.WorkoutEditPage
import com.luckyzero.tacotrainer.ui.pages.WorkoutExecutePage
import com.luckyzero.tacotrainer.ui.pages.WorkoutListPage

@Composable
fun NavigationSystem() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = WorkoutList) {
        composable<WorkoutList> {
            WorkoutListPage(navController, modifier = Modifier)
        }
        composable<WorkoutEdit> {
            val args = it.toRoute<WorkoutEdit>()
            WorkoutEditPage(args, navController, modifier = Modifier)
        }
        composable<WorkoutExecute> {
            var args = it.toRoute<WorkoutExecute>()
            WorkoutExecutePage(args, navController, modifier = Modifier)
        }
    }
}


