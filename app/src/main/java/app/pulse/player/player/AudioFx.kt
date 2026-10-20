package app.pulse.player.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import app.pulse.player.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqBand(val index: Int, val centerHz: Int, val level: Int)

data class FxState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val bands: List<EqBand> = emptyList(),
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500,
    val presets: List<String> = emptyList(),
    val bassSupported: Boolean = false,
    val bassStrength: Int = 0,
)

/** Equalizer and bass boost attached to the player's audio session; settings survive restarts. */
object AudioFx {
    private lateinit var prefs: Prefs
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private val _state = MutableStateFlow(FxState())
    val state: StateFlow<FxState> = _state.asStateFlow()

    fun init(prefs: Prefs) {
        this.prefs = prefs
    }

    @Synchronized
    fun attach(audioSessionId: Int) {
        release()
        equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()?.also { eq ->
            for (band in 0 until eq.numberOfBands) {
                prefs.eqLevel(band)?.let { eq.setBandLevel(band.toShort(), it.toShort()) }
            }
            eq.enabled = prefs.eqEnabled
        }
        bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()?.also { bb ->
            if (bb.strengthSupported) bb.setStrength(prefs.bassStrength.toShort())
            bb.enabled = prefs.eqEnabled && prefs.bassStrength > 0
        }
        publish()
    }

    @Synchronized
    fun release() {
        equalizer?.release()
        bassBoost?.release()
        equalizer = null
        bassBoost = null
        publish()
    }

    fun setEnabled(enabled: Boolean) {
        prefs.eqEnabled = enabled
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled && prefs.bassStrength > 0 }
        publish()
    }

    fun setBand(band: Int, level: Int) {
        prefs.setEqLevel(band, level)
        runCatching { equalizer?.setBandLevel(band.toShort(), level.toShort()) }
        publish()
    }

    fun usePreset(index: Int) {
        val eq = equalizer ?: return
        runCatching { eq.usePreset(index.toShort()) }
        for (band in 0 until eq.numberOfBands) prefs.setEqLevel(band, eq.getBandLevel(band.toShort()).toInt())
        publish()
    }

    fun setBass(strength: Int) {
        prefs.bassStrength = strength
        runCatching {
            bassBoost?.setStrength(strength.toShort())
            bassBoost?.enabled = prefs.eqEnabled && strength > 0
        }
        publish()
    }

    fun reset() {
        val eq = equalizer
        if (eq != null) {
            for (band in 0 until eq.numberOfBands) runCatching { eq.setBandLevel(band.toShort(), 0) }
            prefs.clearEqLevels(eq.numberOfBands.toInt())
        }
        setBass(0)
    }

    private fun publish() {
        val eq = equalizer
        val bb = bassBoost
        _state.value = if (eq == null) {
            FxState(enabled = prefs.eqEnabled)
        } else runCatching {
            val range = eq.bandLevelRange
            FxState(
                available = true,
                enabled = prefs.eqEnabled,
                bands = (0 until eq.numberOfBands).map { b ->
                    EqBand(b, eq.getCenterFreq(b.toShort()) / 1000, eq.getBandLevel(b.toShort()).toInt())
                },
                minLevel = range[0].toInt(),
                maxLevel = range[1].toInt(),
                presets = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) },
                bassSupported = bb?.strengthSupported == true,
                bassStrength = prefs.bassStrength,
            )
        }.getOrElse { FxState(enabled = prefs.eqEnabled) }
    }
}
