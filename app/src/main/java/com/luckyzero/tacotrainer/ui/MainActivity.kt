package com.luckyzero.tacotrainer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.luckyzero.tacotrainer.ui.navigation.NavigationSystem
import com.luckyzero.tacotrainer.ui.theme.TacoTrainerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // TODO: Add a back button to the top bar. This requires moving the nav host up
        // here (or moving some of this logic down, or both.)
        setContent {
            val navHostController = rememberNavController()
            MainContent(navHostController)
        }
    }
}

// TODO:
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(navHostController: NavHostController) {
    // TODO: This doesn't work, I never see updates to the backstack entry.
    // The problem is that it's not a flow.
    // To fix this, I think I'd need a separate mutable state that the lower pages
    // could update themselves.
    val isBackButtonVisible by remember {
        derivedStateOf {
            navHostController.previousBackStackEntry != null
        }
    }

    TacoTrainerTheme(isDynamicColor = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("Taco Trainer")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (navHostController.previousBackStackEntry != null) {
                                navHostController.popBackStack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavigationSystem(navHostController)
            }
        }
    }
}
