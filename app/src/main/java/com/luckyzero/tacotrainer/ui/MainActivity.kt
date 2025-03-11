package com.luckyzero.tacotrainer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import com.luckyzero.tacotrainer.ui.navigation.NavigationSystem
import com.luckyzero.tacotrainer.ui.theme.TacoTrainerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // TODO: Add a back button to the top bar. This requires moving the nav host up
        // here (or moving some of this logic down, or both.)
        setContent {
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
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) {innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavigationSystem()
                    }
                }
            }
        }
    }
}
