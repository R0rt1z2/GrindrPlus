package com.grindrplus.manager

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.grindrplus.manager.ui.HomeScreen
import com.grindrplus.manager.ui.InstallPage
import com.grindrplus.manager.ui.theme.GrindrPlusTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

internal val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
internal const val TAG = "GrindrPlus"
internal const val DATA_URL =
    "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/refs/heads/master/manager-data.json"

sealed class MainNavItem(
    val icon: ImageVector? = null,
    var title: String,
    val composable: @Composable PaddingValues.(Activity) -> Unit,
) {
    data object Settings : MainNavItem(Icons.Filled.Settings, "Settings", { })
    data object InstallPage :
        MainNavItem(Icons.Rounded.Download, "Install", { InstallPage(it, this) })

    data object Home : MainNavItem(Icons.Rounded.Home, "Home", { HomeScreen(this) })
    data object Albums : MainNavItem(Icons.Rounded.PhotoAlbum, "Albums", { })
    data object Experiments : MainNavItem(Icons.Rounded.Science, "Experiments", { })

    companion object {
        val VALUES by lazy { listOf(Settings, InstallPage, Home, Albums, Experiments) }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            GrindrPlusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    Scaffold(
                        topBar = {
                        },
                        content = { innerPadding ->
                            NavHost(
                                navController,
                                startDestination = MainNavItem.InstallPage.toString()
                            ) {
                                for (item in MainNavItem.VALUES) {
                                    composable(item.toString()) {
                                        item.composable(
                                            innerPadding,
                                            this@MainActivity
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            BottomAppBar(modifier = Modifier) {
                                var selectedItem by remember { mutableIntStateOf(0) }
                                var currentRoute by remember { mutableStateOf(MainNavItem.InstallPage.toString()) }

                                MainNavItem.VALUES.forEachIndexed { index, navigationItem ->
                                    if (navigationItem.toString() == currentRoute) {
                                        selectedItem = index
                                    }
                                }

                                NavigationBar {
                                    MainNavItem.VALUES.forEachIndexed { index, item ->
                                        NavigationBarItem(
                                            alwaysShowLabel = true,
                                            icon = {
                                                Icon(
                                                    item.icon!!,
                                                    contentDescription = item.title
                                                )
                                            },
                                            label = { Text(item.title) },
                                            selected = selectedItem == index,
                                            onClick = {
                                                selectedItem = index
                                                currentRoute = item.toString()
                                                navController.navigate(item.toString()) {
                                                    navController.graph.startDestinationRoute?.let { route ->
                                                        popUpTo(route) {
                                                            saveState = true
                                                        }
                                                    }

                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }


    override fun onDestroy() {
        activityScope.cancel() // I always forget about this
        super.onDestroy()
    }
}

