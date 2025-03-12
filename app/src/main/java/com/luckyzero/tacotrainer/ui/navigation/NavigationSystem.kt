package com.luckyzero.tacotrainer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.luckyzero.tacotrainer.ui.pages.WorkoutEditPage
import com.luckyzero.tacotrainer.ui.pages.WorkoutExecutePage
import com.luckyzero.tacotrainer.ui.pages.WorkoutListPage

@Composable
fun NavigationSystem(navHostController: NavHostController) {
    NavHost(navHostController, startDestination = WorkoutList) {
        composable<WorkoutList> {
            WorkoutListPage(navHostController, modifier = Modifier)
        }
        composable<WorkoutEdit> {
            val args = it.toRoute<WorkoutEdit>()
            WorkoutEditPage(args, navHostController, modifier = Modifier)
        }
        composable<WorkoutExecute> {
            val args = it.toRoute<WorkoutExecute>()
            WorkoutExecutePage(args, navHostController, modifier = Modifier)
        }
    }
}


