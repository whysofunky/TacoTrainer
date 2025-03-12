package com.luckyzero.tacotrainer.ui.widgets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.luckyzero.tacotrainer.R

data class TopBarConfig(
    val show: Boolean = true,
    val title: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopBar(navHostController: NavHostController,
                  config: TopBarConfig) {
    val context = LocalContext.current
    var canPop by remember { mutableStateOf(false) }
    navHostController.addOnDestinationChangedListener { controller, _, _ ->
        canPop = controller.previousBackStackEntry != null
    }
    if (config.show) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            title = {
                Text(config.title ?: stringResource(R.string.app_name))
            },
            navigationIcon = {
                if (canPop) {
                    IconButton(onClick = {
                        if (navHostController.previousBackStackEntry != null) {
                            navHostController.popBackStack()
                        } else {
                            error("Back button click, but there's nowhere to go back to.")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                } else {
                    IconButton(onClick = {
                        // TODO: Open menu
                        debugToast(context, "TODO: Show main menu")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Back"
                        )
                    }
                }
            }
        )
    }
}
