package com.metrolist.music.ui.player

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.DefaultNightFrom
import com.metrolist.music.constants.DefaultNightTo
import com.metrolist.music.constants.PlayerDesign
import com.metrolist.music.constants.PlayerDesignKey
import com.metrolist.music.constants.PlayerNightFromKey
import com.metrolist.music.constants.PlayerNightModeKey
import com.metrolist.music.constants.PlayerNightThemeKey
import com.metrolist.music.constants.PlayerNightToKey
import com.metrolist.music.constants.PlayerThemeGalleryKey
import com.metrolist.music.constants.PlayerThemeKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay
import java.util.Calendar

/** The preset a saved design choice (from before the builder existed) stands for. */
internal val PlayerDesign.preset: PlayerTheme
    get() = when (this) {
        PlayerDesign.CLEAN -> PlayerTheme.Clean
        PlayerDesign.RADIO -> PlayerTheme.OnAir
        PlayerDesign.SOFT -> PlayerTheme.Light
    }

/** The player theme drawn right now: the main one, or the evening one when its time has come. */
@Composable
fun rememberPlayerTheme(): PlayerTheme {
    val day = rememberDayPlayerTheme()
    val night = rememberNightPlayerTheme()
    return if (night != null && rememberIsNight()) night else day
}

/** The main theme, as edited in the builder. */
@Composable
fun rememberDayPlayerTheme(): PlayerTheme {
    val json by rememberPreference(PlayerThemeKey, "")
    val design by rememberEnumPreference(PlayerDesignKey, PlayerDesign.CLEAN)
    return remember(json, design) { PlayerTheme.fromJson(json)?.sanitized() ?: design.preset }
}

/** The evening theme, or null while switching is off. */
@Composable
fun rememberNightPlayerTheme(): PlayerTheme? {
    val mode by rememberEnumPreference(PlayerNightModeKey, NightMode.OFF)
    val json by rememberPreference(PlayerNightThemeKey, "")
    val day = rememberDayPlayerTheme()
    if (mode == NightMode.OFF) return null
    return remember(json, day) { PlayerTheme.fromJson(json)?.sanitized() ?: day.nightVersion() }
}

@Composable
fun rememberIsNight(): Boolean {
    val mode by rememberEnumPreference(PlayerNightModeKey, NightMode.OFF)
    val from by rememberPreference(PlayerNightFromKey, DefaultNightFrom)
    val to by rememberPreference(PlayerNightToKey, DefaultNightTo)
    val systemDark = isSystemInDarkTheme()
    val minutes by produceState(minutesNow(), mode) {
        // Ticks only for the clock schedule; wakes at the start of each minute.
        while (mode == NightMode.HOURS) {
            value = minutesNow()
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    return when (mode) {
        NightMode.OFF -> false
        NightMode.SYSTEM -> systemDark
        NightMode.HOURS -> isNightTime(minutes, from, to)
    }
}

private fun minutesNow(): Int = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

@Composable
fun rememberThemeGallery(): List<SavedTheme> {
    val json by rememberPreference(PlayerThemeGalleryKey, "")
    return remember(json) { PlayerThemeGallery.fromJson(json) }
}

suspend fun Context.savePlayerTheme(theme: PlayerTheme) {
    dataStore.edit { it[PlayerThemeKey] = theme.sanitized().toJson() }
}

suspend fun Context.saveNightPlayerTheme(theme: PlayerTheme) {
    dataStore.edit { it[PlayerNightThemeKey] = theme.sanitized().toJson() }
}

suspend fun Context.editThemeGallery(transform: (List<SavedTheme>) -> List<SavedTheme>) {
    dataStore.edit { prefs ->
        prefs[PlayerThemeGalleryKey] = PlayerThemeGallery.toJson(transform(PlayerThemeGallery.fromJson(prefs[PlayerThemeGalleryKey])))
    }
}
