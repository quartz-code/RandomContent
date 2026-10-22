package com.metrolist.music.ui.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.PlayerDesign
import com.metrolist.music.constants.PlayerDesignKey
import com.metrolist.music.constants.PlayerThemeKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

/** The preset a saved design choice (from before the builder existed) stands for. */
internal val PlayerDesign.preset: PlayerTheme
    get() = when (this) {
        PlayerDesign.CLEAN -> PlayerTheme.Clean
        PlayerDesign.RADIO -> PlayerTheme.OnAir
        PlayerDesign.SOFT -> PlayerTheme.Light
    }

/** The player theme in use, following every change made in the builder. */
@Composable
fun rememberPlayerTheme(): PlayerTheme {
    val json by rememberPreference(PlayerThemeKey, "")
    val design by rememberEnumPreference(PlayerDesignKey, PlayerDesign.CLEAN)
    return remember(json, design) { PlayerTheme.fromJson(json)?.sanitized() ?: design.preset }
}

suspend fun Context.savePlayerTheme(theme: PlayerTheme) {
    dataStore.edit { it[PlayerThemeKey] = theme.sanitized().toJson() }
}
