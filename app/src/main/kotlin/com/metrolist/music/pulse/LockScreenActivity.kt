package com.metrolist.music.pulse

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.LockScreenPlayerKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.MusicService.MusicBinder
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.player.LockScreenPlayer
import com.metrolist.music.ui.player.rememberPlayerTheme
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Pulse: the player shown over the lock screen. Opened by [LockScreenLauncher] when the screen
 * turns off during playback; it closes itself once the phone is unlocked.
 */
@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicBinder) {
                playerConnection = PlayerConnection(this@LockScreenActivity, service, database, lifecycleScope)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            finish()
        }
    }

    // Unlocked with a fingerprint or face while this screen was up: get out of the way.
    private val unlocked = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ContextCompat.registerReceiver(this, unlocked, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED)
        bound = bindService(Intent(this, MusicService::class.java), serviceConnection, 0)
        if (!bound) {
            finish()
            return
        }

        setContent {
            val light = rememberPlayerTheme().light
            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = light
            }
            MaterialTheme(colorScheme = darkColorScheme()) {
                playerConnection?.let { connection ->
                    CompositionLocalProvider(LocalPlayerConnection provides connection) {
                        LockScreenPlayer(onUnlock = ::unlock)
                    }
                }
            }
        }
    }

    private fun unlock() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard != null && keyguard.isKeyguardLocked) {
            keyguard.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        finish()
                    }
                },
            )
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlocked) }
        playerConnection?.dispose()
        if (bound) runCatching { unbindService(serviceConnection) }
        super.onDestroy()
    }
}

/**
 * Opens [LockScreenActivity] when the screen goes off while music plays. Android 10+ lets an app
 * start a screen from the background only with "Display over other apps", so without it nothing
 * happens and the setting explains why.
 */
class LockScreenLauncher(private val service: MusicService) : BroadcastReceiver() {
    fun register() {
        ContextCompat.registerReceiver(service, this, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun unregister() {
        runCatching { service.unregisterReceiver(this) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!context.dataStore.get(LockScreenPlayerKey, false)) return
        if (!service.player.isPlaying) return
        if (!canOpen(context)) return
        runCatching {
            context.startActivity(
                Intent(context, LockScreenActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
            )
        }
    }

    companion object {
        fun canOpen(context: Context): Boolean = Settings.canDrawOverlays(context)
    }
}
