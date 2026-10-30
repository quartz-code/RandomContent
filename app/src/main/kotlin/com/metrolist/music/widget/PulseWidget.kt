package com.metrolist.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.widget.RemoteViews
import com.metrolist.music.R
import com.metrolist.music.playback.MusicService

/**
 * Pulse: "My pulse" on the home screen. A tap starts the pulse, or pauses and resumes it while
 * it plays, without opening the app. The heart is drawn here as a picture — a home screen widget
 * cannot animate — in the logo's colours, with the playing song's artwork inside.
 */
class PulseWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Until the player says otherwise, the widget invites to listen; the service updates it once it runs.
        val views = PulseWidget.views(context, null, null, null, pulsePlaying = false, isPlaying = false)
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
        if (MusicService.isRunning) {
            runCatching { context.startService(Intent(context, MusicService::class.java).setAction(MusicWidgetReceiver.ACTION_UPDATE_WIDGET)) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == PulseWidget.ACTION_TAP) {
            // A tap on a widget may start the player's service, as the other widgets do.
            runCatching { context.startService(Intent(context, MusicService::class.java).setAction(PulseWidget.ACTION_TAP)) }
        }
    }
}

object PulseWidget {
    const val ACTION_TAP = "com.metrolist.music.widget.PULSE_TAP"

    private const val SIZE = 360
    private val coral = 0xFFFF4F7B.toInt()
    private val amber = 0xFFFFA24C.toInt()
    private val ink = 0xFF111017.toInt()

    fun update(context: Context, title: String?, artist: String?, artwork: Bitmap?, pulsePlaying: Boolean, isPlaying: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, PulseWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        val views = views(context, title, artist, artwork, pulsePlaying, isPlaying)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    fun views(context: Context, title: String?, artist: String?, artwork: Bitmap?, pulsePlaying: Boolean, isPlaying: Boolean): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_pulse).apply {
            setImageViewBitmap(R.id.widget_pulse_heart, heart(artwork.takeIf { pulsePlaying }, pulsePlaying, isPlaying))
            setTextViewText(R.id.widget_pulse_title, if (pulsePlaying && title != null) title else context.getString(R.string.my_pulse))
            setTextViewText(
                R.id.widget_pulse_subtitle,
                when {
                    pulsePlaying && artist != null -> artist
                    else -> context.getString(R.string.my_pulse_widget_listen)
                },
            )
            val tap = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, PulseWidgetReceiver::class.java).setAction(ACTION_TAP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            setOnClickPendingIntent(R.id.widget_pulse_root, tap)
        }

    /** The page's heart, still: a glow, two rings, and inside the artwork or the play sign. */
    private fun heart(artwork: Bitmap?, pulsePlaying: Boolean, isPlaying: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val c = SIZE / 2f
        val gradient = LinearGradient(0f, SIZE.toFloat(), SIZE.toFloat(), 0f, coral, amber, Shader.TileMode.CLAMP)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = RadialGradient(c, c, c, intArrayOf(0x88FF4F7B.toInt(), 0x22FFA24C, 0), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(c, c, c, paint)

        paint.shader = gradient
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = SIZE * 0.022f
        paint.alpha = 110
        canvas.drawCircle(c, c, SIZE * 0.45f, paint)
        paint.alpha = 255
        paint.strokeWidth = SIZE * 0.03f
        canvas.drawCircle(c, c, SIZE * 0.38f, paint)

        paint.style = Paint.Style.FILL
        val inner = SIZE * 0.32f
        canvas.drawCircle(c, c, inner, paint)

        if (artwork != null) {
            // The artwork in a circle inside the coral ring, as on the page.
            val r = inner * 0.86f
            val scale = (2 * r) / minOf(artwork.width, artwork.height)
            val shader = BitmapShader(artwork, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setLocalMatrix(
                Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(c - artwork.width * scale / 2, c - artwork.height * scale / 2)
                },
            )
            canvas.drawCircle(c, c, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
            if (!isPlaying) {
                canvas.drawCircle(c, c, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x73111017 })
                drawPlay(canvas, c, r * 0.42f, 0xFFFFFFFF.toInt())
            }
        } else if (pulsePlaying && isPlaying) {
            drawPause(canvas, c, inner * 0.42f)
        } else {
            drawPlay(canvas, c, inner * 0.42f, ink)
        }
        return bitmap
    }

    private fun drawPlay(canvas: Canvas, c: Float, size: Float, color: Int) {
        val path = Path().apply {
            moveTo(c - size * 0.55f, c - size)
            lineTo(c + size, c)
            lineTo(c - size * 0.55f, c + size)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            pathEffect = android.graphics.CornerPathEffect(size * 0.18f)
        })
    }

    private fun drawPause(canvas: Canvas, c: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        val w = size * 0.42f
        canvas.drawRoundRect(c - size * 0.6f, c - size, c - size * 0.6f + w, c + size, w / 2, w / 2, paint)
        canvas.drawRoundRect(c + size * 0.6f - w, c - size, c + size * 0.6f, c + size, w / 2, w / 2, paint)
    }
}
