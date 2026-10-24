package app.pulse.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.metrolist.music.ui.player.PlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Pulse desktop: the recap card as a PNG, for sending where the phone would "share". */
object RecapImage {
    /** The card's own size at twice the density: 1080×1350. */
    private const val DENSITY = 2f
    private val WIDTH_PX = (CARD_WIDTH.value * DENSITY).toInt()
    private val HEIGHT_PX = (CARD_HEIGHT.value * DENSITY).toInt()

    /** Asks where to save, then draws the card off screen. Null when the user cancels. */
    suspend fun save(recap: Recaps.Recap, theme: PlayerTheme, fonts: ThemeFonts): Result<File>? {
        val target = chooseFile("Pulse-${recap.period.key}.png") ?: return null
        return withContext(Dispatchers.Default) {
            runCatching {
                target.writeBytes(render(recap, theme, fonts))
                target
            }
        }
    }

    suspend fun render(recap: Recaps.Recap, theme: PlayerTheme, fonts: ThemeFonts): ByteArray {
        val scene = ImageComposeScene(WIDTH_PX, HEIGHT_PX, Density(DENSITY)) {
            MaterialTheme(colorScheme = darkColorScheme()) { RecapCard(recap, theme, fonts) }
        }
        try {
            // Covers come in through Coil a moment later (usually from its memory cache): draw a few frames.
            var image = scene.render(0)
            for (frame in 1..25) {
                delay(100)
                image = scene.render(frame * 100_000_000L)
            }
            return requireNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes
        } finally {
            scene.close()
        }
    }

    /** The system's own save dialog, in Pictures when there is one. Runs on the window's thread. */
    private fun chooseFile(name: String): File? {
        val dialog = FileDialog(null as Frame?, Strings.saveImage, FileDialog.SAVE)
        val home = File(System.getProperty("user.home"))
        dialog.directory = listOf(File(home, "Pictures"), File(home, "Изображения"), home).first { it.isDirectory }.absolutePath
        dialog.file = name
        dialog.isVisible = true
        val file = dialog.file ?: return null
        val chosen = File(dialog.directory, file)
        return if (chosen.name.endsWith(".png", ignoreCase = true)) chosen else File(chosen.parentFile, chosen.name + ".png")
    }
}
