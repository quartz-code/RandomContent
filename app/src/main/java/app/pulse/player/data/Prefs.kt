package app.pulse.player.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("pulse", Context.MODE_PRIVATE)

    var parallelDownloads: Int
        get() = sp.getInt("parallel", 3)
        set(v) = sp.edit { putInt("parallel", v) }

    /** Delete local files of tracks that are no longer liked on YouTube Music. */
    var deleteUnliked: Boolean
        get() = sp.getBoolean("delete_unliked", false)
        set(v) = sp.edit { putBoolean("delete_unliked", v) }

    var wifiOnly: Boolean
        get() = sp.getBoolean("wifi_only", false)
        set(v) = sp.edit { putBoolean("wifi_only", v) }

    var autoSync: Boolean
        get() = sp.getBoolean("auto_sync", false)
        set(v) = sp.edit { putBoolean("auto_sync", v) }

    var lastSync: Long
        get() = sp.getLong("last_sync", 0)
        set(v) = sp.edit { putLong("last_sync", v) }

    var lastSyncResult: String?
        get() = sp.getString("last_sync_result", null)
        set(v) = sp.edit { putString("last_sync_result", v) }

    var lastEngineUpdate: Long
        get() = sp.getLong("last_engine_update", 0)
        set(v) = sp.edit { putLong("last_engine_update", v) }

    var librarySort: String
        get() = sp.getString("library_sort", "liked")!!
        set(v) = sp.edit { putString("library_sort", v) }

    // Audio effects
    var eqEnabled: Boolean
        get() = sp.getBoolean("eq_enabled", false)
        set(v) = sp.edit { putBoolean("eq_enabled", v) }

    fun eqLevel(band: Int): Int? = if (sp.contains("eq_band_$band")) sp.getInt("eq_band_$band", 0) else null
    fun setEqLevel(band: Int, level: Int) = sp.edit { putInt("eq_band_$band", level) }
    fun clearEqLevels(bands: Int) = sp.edit { repeat(bands) { remove("eq_band_$it") } }

    var bassStrength: Int
        get() = sp.getInt("bass", 0)
        set(v) = sp.edit { putInt("bass", v) }
}
