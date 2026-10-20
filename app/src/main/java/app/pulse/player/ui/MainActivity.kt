package app.pulse.player.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.pulse.player.player.PlayerConnection

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("library", "Библиотека", Icons.Rounded.LibraryMusic),
    Tab("playlists", "Плейлисты", Icons.AutoMirrored.Rounded.QueueMusic),
    Tab("sync", "Синхронизация", Icons.Rounded.Sync),
)

/** Full-screen routes that hide the bottom bar and mini player. */
private val fullScreen = setOf("player", "equalizer", "login")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PlayerConnection.connect(this)
        setContent { PulseTheme { App() } }
    }
}

@Composable
private fun App() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun openTab(tab: String) = nav.navigate(tab) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        bottomBar = {
            if (route !in fullScreen) {
                Column {
                    MiniPlayer(onOpen = { nav.navigate("player") })
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = backStack?.destination?.hierarchy()?.contains(tab.route) == true ||
                                (tab.route == "playlists" && route?.startsWith("playlist/") == true)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { openTab(tab.route) },
                                icon = { Icon(tab.icon, null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "library",
            modifier = when (route) {
                "player" -> Modifier
                in fullScreen -> Modifier.systemBarsPadding().imePadding()
                else -> Modifier.padding(padding)
            },
        ) {
            composable("library") { LibraryScreen(onOpenSync = { openTab("sync") }) }
            composable("playlists") { PlaylistsScreen(onOpen = { nav.navigate("playlist/$it") }) }
            composable(
                "playlist/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                PlaylistDetailScreen(entry.arguments!!.getLong("id"), onBack = { nav.popBackStack() })
            }
            composable("sync") { SyncScreen(onLogin = { nav.navigate("login") }) }
            composable("login") { LoginScreen(onDone = { nav.popBackStack() }) }
            composable(
                "player",
                enterTransition = { slideInVertically { it } },
                exitTransition = { slideOutVertically { it } },
                popExitTransition = { slideOutVertically { it } },
            ) {
                PlayerScreen(onClose = { nav.popBackStack() }, onEqualizer = { nav.navigate("equalizer") })
            }
            composable("equalizer") { EqualizerScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

private fun androidx.navigation.NavDestination.hierarchy(): List<String> =
    generateSequence(this) { it.parent }.mapNotNull { it.route }.toList()
