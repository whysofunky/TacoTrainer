package com.luckyzero.tacotrainer.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.luckyzero.tacotrainer.ui.navigation.NavigationSystem
import com.luckyzero.tacotrainer.ui.theme.TacoTrainerTheme
import com.luckyzero.tacotrainer.ui.widgets.DynamicFooter
import com.luckyzero.tacotrainer.ui.widgets.DynamicFooterConfig
import com.luckyzero.tacotrainer.ui.widgets.DynamicTopBar
import com.luckyzero.tacotrainer.ui.widgets.TopBarConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // TODO: Add a back button to the top bar. This requires moving the nav host up
        // here (or moving some of this logic down, or both.)
        setContent {
            MainContent()
        }
    }
}

@Composable
private fun MainContent() {
    val navHostController = rememberNavController()
    val topBarConfig by remember { mutableStateOf(TopBarConfig()) }
    TacoTrainerTheme(isDynamicColor = false) {
        Scaffold(
            topBar = { DynamicTopBar(navHostController, topBarConfig) },
            bottomBar = { DynamicFooter(DynamicFooterConfig()) },
            modifier = Modifier.fillMaxSize()
        ) {innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavigationSystem(navHostController, topBarConfig)
            }
        }
    }
}
