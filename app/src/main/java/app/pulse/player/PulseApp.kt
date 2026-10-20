package app.pulse.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import app.pulse.player.data.Prefs
import app.pulse.player.data.PulseDb
import app.pulse.player.player.AudioFx

class PulseApp : Application() {
    val db by lazy { PulseDb.get(this) }
    val prefs by lazy { Prefs(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AudioFx.init(prefs)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(SYNC_CHANNEL, "Синхронизация", NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        const val SYNC_CHANNEL = "sync"
        lateinit var instance: PulseApp
            private set
    }
}
