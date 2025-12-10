package com.grindrplus.manager

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color.TRANSPARENT
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.grindrplus.GrindrPlus
import com.grindrplus.bridge.BridgeClient
import com.grindrplus.bridge.NotificationActionReceiver
import com.grindrplus.core.Config
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.core.Logger
import com.grindrplus.manager.MainNavItem.*
import com.grindrplus.manager.ui.BlockLogScreen
import com.grindrplus.manager.ui.CalculatorScreen
import com.grindrplus.manager.ui.HomeScreen
import com.grindrplus.manager.ui.InstallPage
import com.grindrplus.manager.ui.SettingsScreen
import com.grindrplus.manager.ui.NotificationScreen
import com.grindrplus.manager.ui.theme.GrindrPlusTheme
import com.grindrplus.manager.utils.FileOperationHandler
import com.grindrplus.manager.utils.isLSPosed
import com.grindrplus.utils.HookManager
import com.grindrplus.utils.TaskManager
import com.onebusaway.plausible.android.AndroidResourcePlausibleConfig
import com.onebusaway.plausible.android.NetworkFirstPlausibleClient
import com.onebusaway.plausible.android.Plausible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import timber.log.Timber.DebugTree


internal val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
internal const val TAG = "GrindrPlus"
internal const val DATA_URL =
    "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/refs/heads/master/manifest.json"

sealed class MainNavItem(
    val icon: ImageVector? = null,
    var title: String,
    val composable: @Composable PaddingValues.(Activity) -> Unit,
) {
    data object Settings :
        MainNavItem(Icons.Filled.Settings, "Settings", { SettingsScreen() })

    data object InstallPage :
        MainNavItem(Icons.Rounded.Download, "Install", { InstallPage(it, this) })

    data object Home : MainNavItem(Icons.Rounded.Home, "Home", { HomeScreen(this) })

    data object BlockLog : MainNavItem(Icons.Filled.History, "Block Log", { BlockLogScreen(this) })

    data object Notifications : MainNavItem(Icons.Filled.Newspaper, "News", { NotificationScreen(this) })

    // data object Albums : MainNavItem(Icons.Rounded.PhotoAlbum, "Albums", { ComingSoon() })
    // data object Experiments : MainNavItem(Icons.Rounded.Science, "Experiments", { ComingSoon() })

    companion object {
        val VALUES by lazy {
            listOf(InstallPage, BlockLog, Home, Notifications, Settings)
        }
    }
}

class MainActivity : ComponentActivity() {
    companion object {
        var plausible: Plausible? = null
        val showUninstallDialog = mutableStateOf(false)
    }

    private var showPermissionDialog by mutableStateOf(false)
    private lateinit var receiver: NotificationActionReceiver

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Logger.i("Notification permission granted")
        } else {
            Logger.w("Notification permission denied")
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Logger.d("Notification permission already granted")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionDialog = true
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun checkUnknownSourcesPermission() {
        var allow = false
        allow = packageManager.canRequestPackageInstalls()

        if (!allow) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:$packageName".toUri()
            }
            Toast.makeText(
                this,
                "Please allow unknown sources for GrindrPlus",
                Toast.LENGTH_LONG
            ).show()
            startActivity(intent)
        }
    }

    private fun registerNotificationReceiver() {
        try {
            receiver = NotificationActionReceiver()
            val intentFilter = IntentFilter().apply {
                addAction("com.grindrplus.COPY_ACTION")
                addAction("com.grindrplus.VIEW_PROFILE_ACTION")
                addAction("com.grindrplus.CUSTOM_ACTION")
                addAction("com.grindrplus.DEFAULT_ACTION")
            }
            ContextCompat.registerReceiver(
                applicationContext,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            Logger.i("Registered notification action receiver")
        } catch (e: Exception) {
            Logger.e("Failed to register receiver: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(DebugTree())
        FileOperationHandler.init(this)
        registerNotificationReceiver()

        val isSystemInDarkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val statusBarStyle = if (isSystemInDarkTheme) {
            SystemBarStyle.dark(TRANSPARENT)
        } else {
            SystemBarStyle.light(TRANSPARENT, TRANSPARENT)
        }

        val navigationBarStyle = if (isSystemInDarkTheme) {
            SystemBarStyle.dark(TRANSPARENT)
        } else {
            SystemBarStyle.light(TRANSPARENT, TRANSPARENT)
        }

        enableEdgeToEdge(
            statusBarStyle = statusBarStyle,
            navigationBarStyle = navigationBarStyle
        )

        setContent {
            var serviceBound by remember { mutableStateOf(false) }
            var firstLaunchDialog by remember { mutableStateOf(false) }
            var patchInfoDialog by remember { mutableStateOf(false) }
            var showUninstallDialogState by remember { showUninstallDialog }
            var calculatorScreen = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                GrindrPlus.bridgeClient = BridgeClient(this@MainActivity)
                GrindrPlus.bridgeClient.connectAsync { connected ->
                    Logger.initialize(this@MainActivity, GrindrPlus.bridgeClient, false)
                    Config.initialize()
                    HookManager().registerHooks(false)
                    TaskManager().registerTasks(false)
                    calculatorScreen.value = Config.get("discreet_icon", false) as Boolean
                    serviceBound = true

                    if (!(Config.get("disable_permission_checks", false) as Boolean)) {
                        checkNotificationPermission()
                        checkUnknownSourcesPermission()
                    }

                    if (Config.get("analytics", true) as Boolean) {
                        val config = AndroidResourcePlausibleConfig(this@MainActivity).also {
                            it.domain = "grindrplus.lol"
                            it.host = "https://plausible.gmmz.dev/api/"
                            it.enable = true
                        }

                        plausible = Plausible(
                            config = config,
                            client = NetworkFirstPlausibleClient(config)
                        )

                        plausible?.enable(true)
                        plausible?.pageView(
                            "app://grindrplus/home",
                            props = mapOf("android_version" to Build.VERSION.SDK_INT)
                        )
                    }

                    if (Config.get("first_launch", true) as Boolean) {
                        firstLaunchDialog = true
                        patchInfoDialog = true
                        plausible?.pageView("app://grindrplus/first_launch")
                        Config.put("first_launch", false)

                    }
                }
            }

            if (!serviceBound) {
                return@setContent
            }

            GrindrPlusTheme(
                dynamicColor = Config.get("material_you", false) as Boolean,
            ) {
                if (calculatorScreen.value) {
                    CalculatorScreen(calculatorScreen)
                    return@GrindrPlusTheme
                }

                if (showPermissionDialog) {
                    Dialog(
                        onDismissRequest = { showPermissionDialog = false }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Center
                            ) {
                                Text(
                                    text = "Notification Permission",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = "GrindrPlus needs notification permission to alert you when someone blocks or unblocks you.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showPermissionDialog = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Not now")
                                    }

                                    Button(
                                        onClick = {
                                            showPermissionDialog = false
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Grant Access")
                                    }
                                }
                            }
                        }
                    }
                }

                if (firstLaunchDialog) {
                    Dialog(
                        onDismissRequest = { firstLaunchDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Center
                            ) {
                                Text(
                                    text = "Welcome to GrindrPlus!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text =
                                        "We collect totally anonymous data to improve the app.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text =
                                        "You can disable this in the settings.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = "Data collected:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    text = "• App opens\n• Installation success/failure\n• Eventual failure reason\n• Android version",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Button(
                                    onClick = { firstLaunchDialog = false },
                                    modifier = Modifier
                                        .align(CenterHorizontally)
                                        .padding(top = 16.dp)
                                ) {
                                    Text("Ok, got it")
                                }
                            }
                        }
                    }

                    return@GrindrPlusTheme
                }

                if (showUninstallDialogState) {
                    Dialog(
                        onDismissRequest = { showUninstallDialogState = false }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Center
                            ) {
                                Text(
                                    text = "Installation Error",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = "The installation failed because the app signatures don't match.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = "Please uninstall Grindr manually first, then try the installation again.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
                                    Text(
                                        text = "If you have Grindr installed in the Secure Folder, PLEASE UNINSTALL IT from there as well.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showUninstallDialogState = false },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }

                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DELETE)
                                                intent.data = "package:$GRINDR_PACKAGE_NAME".toUri()
                                                startActivity(intent)
                                                showUninstallDialogState = false
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Error: ${e.localizedMessage}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Uninstall")
                                    }
                                }
                            }
                        }
                    }
                }

                if (patchInfoDialog) {
                    Dialog(
                        onDismissRequest = { patchInfoDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Center
                            ) {
                                Text(
                                    text = "Installation Method",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = "• If you were using LSPatch previously, go to the Install section and install the latest version.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Text(
                                    text = buildAnnotatedString {
                                        append("• If you were using LSPosed, make sure the module is enabled in the LSPosed manager and Grindr app is within its scope. ")

                                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                                            append("Do not use the Install section if you're using LSPosed.")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Button(
                                    onClick = { patchInfoDialog = false },
                                    modifier = Modifier
                                        .align(CenterHorizontally)
                                        .padding(top = 8.dp)
                                ) {
                                    Text("Understood")
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val navController = rememberNavController()

                    Scaffold(
                        topBar = {
                        },
                        content = { innerPadding ->
                            NavHost(
                                navController,
                                startDestination = Home.toString()
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
                                var currentRoute =
                                    navController.currentBackStackEntryAsState().value?.destination?.route
                                        ?: Home.toString()

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
                                                navController.navigateItem(item)
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
        activityScope.cancel()
        try {
            if (::receiver.isInitialized) {
                applicationContext.unregisterReceiver(receiver)
                Logger.i("Unregistered notification action receiver")
            }
        } catch (e: Exception) {
            Logger.e("Error unregistering receiver: ${e.message}")
        }
        super.onDestroy()
    }
}

fun NavController.navigateItem(item: MainNavItem) {
    navigate(item.toString()) {
        graph.startDestinationRoute?.let { route ->
            popUpTo(route) {
                saveState = true
            }
        }

        launchSingleTop = true
        restoreState = true
    }
}

