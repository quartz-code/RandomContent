/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.HideStatusBarOnFullscreenKey
import com.metrolist.music.constants.KeepScreenOn
import com.metrolist.music.constants.PlayerDesign
import com.metrolist.music.constants.PlayerDesignKey
import com.metrolist.music.constants.SleepTimerDefaultKey
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.ui.utils.ShowOffsetDialog
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.safeDataStoreEdit
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import com.metrolist.music.ui.component.Icon as MIcon
import com.metrolist.music.constants.SleepTimerDefaultKey
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val copiedTitleStr = stringResource(R.string.copied_title)
    val copiedArtistStr = stringResource(R.string.copied_artist)
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val playerDesign by rememberEnumPreference(PlayerDesignKey, defaultValue = PlayerDesign.CLEAN)
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (hideStatusBarOnFullscreen) = rememberPreference(HideStatusBarOnFullscreenKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)

    var showInlineLyrics by rememberSaveable {
        mutableStateOf(false)
    }

    var isFullScreen by rememberSaveable {
        mutableStateOf(false)
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    DisposableEffect(state.isExpanded, useDarkTheme, keepScreenOn, isFullScreen, hideStatusBarOnFullscreen, playerDesign) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            // Pulse: the status bar follows the player design, not the app theme.
            insetsController.isAppearanceLightStatusBars = playerDesign.isLight

            if (isFullScreen && hideStatusBarOnFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }

            if (keepScreenOn && state.isExpanded) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    BackHandler(enabled = state.isExpanded) {
        state.collapseSoft()
    }

    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()


    // Listen Together state (reactive)
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if focus request fails
            }
        }
    }

    // Use Cast state when casting, otherwise local player
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    // Use State objects for position/duration to pass to MiniPlayer without causing recomposition
    // These states persist across playback state changes to ensure continuous progress updates.
    // Seed from the player's current values so re-entering composition on resume shows the real
    // time immediately instead of flashing 0:00 until the first poll fires. runCatching guards the
    // player-not-ready race; the poll loop corrects duration if it isn't known yet.
    val positionState = remember { mutableLongStateOf(runCatching { playerConnection.player.currentPosition }.getOrDefault(0L)) }
    val durationState = remember {
        mutableLongStateOf(
            (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: runCatching { playerConnection.player.duration }.getOrDefault(0L).coerceAtLeast(0L),
        )
    }

    // Convenience accessors for local use
    var position by positionState
    var duration by durationState

    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) {
                castPosition
            } else {
                position
            }
        }
    }

    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    // Track when we last manually set position to avoid Cast overwriting it
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerDesign) {
        if (playerDesign == PlayerDesign.CLEAN || playerDesign == PlayerDesign.SOFT) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(currentMetadata.thumbnailUrl)
                            .size(100, 100)
                            .allowHardware(false)
                            .memoryCacheKey("gradient_${currentMetadata.id}")
                            .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette =
                                withContext(Dispatchers.Default) {
                                    Palette
                                        .from(bitmap)
                                        .maximumColorCount(8)
                                        .resizeBitmapArea(100 * 100)
                                        .generate()
                                }
                            val extractedColors =
                                PlayerColorExtractor.extractGradientColors(
                                    palette = palette,
                                    fallbackColor = fallbackColor,
                                )
                            gradientColorsCache[currentMetadata.id] = extractedColors
                            withContext(Dispatchers.Main) { gradientColors = extractedColors }
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val download by LocalDownloadUtil.current
        .getDownload(mediaMetadata?.id ?: "")
        .collectAsStateWithLifecycle(initialValue = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer?.triggerTime,
            playerConnection.service.sleepTimer?.pauseWhenSongEnd,
        ) {
            playerConnection.service.sleepTimer?.isActive ?: false
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer?.pauseWhenSongEnd == true) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        (playerConnection.service.sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    val scope = rememberCoroutineScope()
    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember {
        derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() }
    }
    LaunchedEffect(sleepTimerDefault) { sleepTimerValue = sleepTimerDefault }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)


    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer?.start(
                            minute = sleepTimerValue.roundToInt(),
                            stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                            fadeOut = sleepTimerFadeOut,
                        )
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.minute,
                                sleepTimerValue.roundToInt(),
                                sleepTimerValue.roundToInt(),
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (isAtDefault) {
                            FilledIconButton(
                                onClick = {
                                    scope.launch {
                                        context.safeDataStoreEdit { settings ->
                                            settings[SleepTimerDefaultKey] = sleepTimerValue
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Text(stringResource(R.string.set_as_default))
                            }
                        } else {
                            OutlinedIconButton(
                                onClick = {
                                    scope.launch {
                                        context.safeDataStoreEdit { settings ->
                                            settings[SleepTimerDefaultKey] = sleepTimerValue
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                            ) {
                                Text(stringResource(R.string.set_as_default))
                            }
                        }

                        OutlinedIconButton(
                            onClick = {
                                showSleepTimerDialog = false
                                playerConnection.service.sleepTimer?.start(minute = -1)
                            },
                        ) {
                            Text(stringResource(R.string.end_of_song))
                        }
                    }
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // Position update - only for local playback
    // When casting, we use castPosition directly to avoid sync issues
    // Use isPlaying instead of playbackState to ensure continuous updates during playback
    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100) // Update more frequently for smoother progress bar
                if (sliderPosition == null) { // Only update if user isn't dragging
                    position = playerConnection.player.currentPosition
                    // Don't clobber a valid (metadata-derived) duration with 0/UNSET mid-resolve.
                    playerConnection.player.duration.takeIf { it > 0 }?.let { duration = it }
                }
            }
        }
    }

    // Also update position when playback state changes (e.g., song change, seek)
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            // Prefer the song's known duration (from metadata, available instantly from the restored
            // queue) so the slider range is right even when restored paused / before the stream
            // resolves; fall back to the player's duration once it is known.
            duration = (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: playerConnection.player.duration
        }
    }

    // Auto-switch from repeat one to repeat all when song ends naturally
    var previousMediaId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playbackState, mediaMetadata?.id) {
        val currentId = mediaMetadata?.id

        // Only switch from REPEAT_ONE to REPEAT_ALL when playback naturally ended
        // (i.e., the player transitioned to ENDED state and then moved to next track).
        // Do NOT switch on manual skips.
        if (currentId != null &&
            currentId != previousMediaId &&
            previousMediaId != null &&
            playbackState == Player.STATE_ENDED &&
            repeatMode == Player.REPEAT_MODE_ONE &&
            !isListenTogetherGuest) {
            playerConnection.player.setRepeatMode(Player.REPEAT_MODE_ALL)
        }

        previousMediaId = currentId
    }

    // When casting, use Cast position/duration directly
    // But wait a bit after manual seeks to let Cast catch up
    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                // Only update from Cast if we haven't manually seeked recently
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }

    val dismissedBound = playerDesign.peekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = dismissedBound,
            expandedBound = state.expandedBound,
            collapsedBound = dismissedBound + 1.dp,
            initialAnchor = 1,
        )

    val bottomSheetBackgroundColor = playerDesign.baseBackground

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    // Pulse: the light design colours its controls from the artwork.
    CompositionLocalProvider(LocalPulseArtworkColor provides gradientColors.firstOrNull()) {
    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(bottomSheetBackgroundColor),
            ) {
                PulsePlayerBackground(
                    design = playerDesign,
                    artworkColors = gradientColors,
                    modifier = Modifier.alpha(backgroundAlpha),
                )
            }
        },
        onDismiss =
            if (!isListenTogetherGuest) {
                {
                    playerConnection.service.clearAutomix()
                    playerConnection.player.stop()
                    playerConnection.player.clearMediaItems()
                }
            } else {
                null
            },
        collapsedContent = {
            MiniPlayer(
                positionState = positionState,
                durationState = durationState,
                onClick = { state.expandSoft() },
            )
        },
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
        val listenTogetherLabel =
            when (listenTogetherRoleState?.value) {
                RoomRole.HOST -> stringResource(R.string.pulse_listen_together_host)
                RoomRole.GUEST -> stringResource(R.string.pulse_listen_together_guest)
                else -> null
            }
        val playerHeader: @Composable () -> Unit = {
            PulsePlayerHeader(
                design = playerDesign,
                listenTogetherLabel = listenTogetherLabel,
                source = queueTitle ?: mediaMetadata?.album?.title,
                onCollapse = { state.collapseSoft() },
                onMore = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = {
                                mediaMetadata?.id?.let { id ->
                                    bottomSheetPageState.show { ShowMediaInfo(id) }
                                }
                            },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
            )
        }

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
            // For episodes, show saved state (inLibrary); for songs, show liked state
            val isEpisode = currentSong?.song?.isEpisode == true
            val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
            val artistsText = mediaMetadata.artists.filter { it.name.isNotBlank() }.joinToString { it.name }

            PulseControlsColumn(design = playerDesign) {
                PulseTrackInfo(
                    design = playerDesign,
                    title = mediaMetadata.title,
                    artists = artistsText,
                    isFavorite = isFavorite,
                    onLike = playerConnection::toggleLike,
                    onTitleClick = {
                        val albumId = mediaMetadata.album?.id
                            ?: currentSong?.album?.id
                            ?: currentSong?.song?.albumId
                        if (albumId != null) {
                            navController.navigate("album/$albumId")
                            state.collapseSoft()
                        }
                    },
                    onTitleLongClick = {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(copiedTitleStr, mediaMetadata.title))
                        Toast.makeText(context, copiedTitleStr, Toast.LENGTH_SHORT).show()
                    },
                    onArtistsClick = {
                        mediaMetadata.artists.firstOrNull { !it.id.isNullOrBlank() }?.id?.let { artistId ->
                            navController.navigate("artist/$artistId")
                            state.collapseSoft()
                        }
                    },
                    onArtistsLongClick = {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(copiedArtistStr, artistsText))
                        Toast.makeText(context, copiedArtistStr, Toast.LENGTH_SHORT).show()
                    },
                    leading =
                        if (showInlineLyrics && !hidePlayerThumbnail) {
                            {
                                AsyncImage(
                                    model = mediaMetadata.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                    modifier =
                                        Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(playerDesign.artCorner / 2)),
                                )
                            }
                        } else {
                            null
                        },
                    trailing =
                        if (showInlineLyrics) {
                            {
                                val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                                PulseTrailingButton(
                                    design = playerDesign,
                                    icon = R.drawable.more_horiz,
                                    onClick = {
                                        menuState.show {
                                            com.metrolist.music.ui.menu.LyricsMenu(
                                                lyricsProvider = { currentLyrics },
                                                songProvider = { currentSong?.song },
                                                mediaMetadataProvider = { mediaMetadata },
                                                onDismiss = menuState::dismiss,
                                                onShowOffsetDialog = {
                                                    bottomSheetPageState.show {
                                                        ShowOffsetDialog(
                                                            songProvider = { currentSong?.song },
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                    },
                                )
                            }
                        } else {
                            null
                        },
                )

                PulseProgress(
                    design = playerDesign,
                    position = sliderPosition ?: effectivePosition,
                    duration = if (duration == C.TIME_UNSET) 0L else duration,
                    enabled = !isListenTogetherGuest,
                    onSeek = { sliderPosition = it },
                    onSeekFinished = {
                        sliderPosition?.let {
                            if (isCasting) {
                                castHandler?.seekTo(it)
                                lastManualSeekTime = System.currentTimeMillis()
                            } else {
                                playerConnection.player.seekTo(it)
                            }
                            position = it
                        }
                        sliderPosition = null
                    },
                )

                PulseTransport(
                    design = playerDesign,
                    playIcon =
                        when {
                            isListenTogetherGuest -> if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                            playbackState == STATE_ENDED -> R.drawable.replay
                            effectiveIsPlaying -> R.drawable.pulse_pause
                            else -> R.drawable.pulse_play
                        },
                    playDescription =
                        when {
                            isListenTogetherGuest -> stringResource(if (isMuted) R.string.unmute else R.string.mute)
                            effectiveIsPlaying -> stringResource(R.string.player_pause)
                            else -> stringResource(R.string.play)
                        },
                    canPrevious = canSkipPrevious && !isListenTogetherGuest,
                    canNext = canSkipNext && !isListenTogetherGuest,
                    controlsEnabled = !isListenTogetherGuest,
                    shuffleOn = shuffleModeEnabled,
                    repeatMode = repeatMode,
                    onPlayPause = {
                        when {
                            isListenTogetherGuest -> playerConnection.toggleMute()
                            isCasting -> if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                            playbackState == STATE_ENDED -> {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            }
                            else -> playerConnection.togglePlayPause()
                        }
                    },
                    onPrevious = playerConnection::seekToPrevious,
                    onNext = playerConnection::seekToNext,
                    onShuffle = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                    onRepeat = { playerConnection.player.toggleRepeatMode() },
                    playModifier = Modifier.focusRequester(focusRequester),
                )
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // Calculate vertical padding like OuterTune
                val density = LocalDensity.current
                val verticalPadding =
                    max(
                        WindowInsets.systemBars.getTop(density),
                        WindowInsets.systemBars.getBottom(density),
                    )
                val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                val verticalWindowInsets = WindowInsets(left = 0.dp, top = verticalPaddingDp, right = 0.dp, bottom = verticalPaddingDp)

                Row(
                    modifier =
                        Modifier
                            .windowInsetsPadding(
                                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(verticalWindowInsets),
                            ).padding(bottom = 24.dp)
                            .fillMaxSize(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .weight(1f)
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                    ) {
                        // Remember lambdas to prevent unnecessary recomposition
                        val currentSliderPosition by rememberUpdatedState(sliderPosition)
                        val sliderPositionProvider = remember { { currentSliderPosition } }
                        val isExpandedProvider = remember(state) { { state.isExpanded } }
                        AnimatedContent(
                            targetState = showInlineLyrics,
                            label = "Lyrics",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                        ) { showLyrics ->
                            if (showLyrics) {
                                InlineLyricsView(
                                    mediaMetadata = mediaMetadata,
                                    showLyrics = showLyrics,
                                    positionProvider = { effectivePosition },
                                )
                            } else {
                                Thumbnail(
                                    sliderPositionProvider = sliderPositionProvider,
                                    modifier = Modifier.animateContentSize(),
                                    isPlayerExpanded = isExpandedProvider,
                                    isLandscape = true,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                    horizontalPadding = playerDesign.artPadding,
                                    cornerRadius = playerDesign.artCorner,
                                    contentColor = playerDesign.palette.content,
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(if (showInlineLyrics) 0.65f else 1f, false)
                                .animateContentSize()
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                val bottomPadding by animateDpAsState(
                    targetValue = if (isFullScreen) 0.dp else queueSheetState.collapsedBound,
                    label = "bottomPadding",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = bottomPadding)
                            .animateContentSize(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        // Remember lambdas to prevent unnecessary recomposition
                        val currentSliderPosition by rememberUpdatedState(sliderPosition)
                        val sliderPositionProvider = remember { { currentSliderPosition } }
                        val isExpandedProvider = remember(state) { { state.isExpanded } }
                        AnimatedContent(
                            targetState = showInlineLyrics,
                            label = "Lyrics",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                        ) { showLyrics ->
                            if (showLyrics) {
                                InlineLyricsView(
                                    mediaMetadata = mediaMetadata,
                                    showLyrics = showLyrics,
                                    positionProvider = { effectivePosition },
                                )
                            } else {
                                Thumbnail(
                                    sliderPositionProvider = sliderPositionProvider,
                                    modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                    isPlayerExpanded = isExpandedProvider,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                    header = playerHeader,
                                    horizontalPadding = playerDesign.artPadding,
                                    cornerRadius = playerDesign.artCorner,
                                    contentColor = playerDesign.palette.content,
                                )
                            }
                        }
                    }

                    mediaMetadata?.let {
                        controlsContent(it)
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = !isFullScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
                background =
                    if (useBlackBackground) {
                        Color.Black
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                pureBlack = pureBlack,
                showInlineLyrics = showInlineLyrics,
                design = playerDesign,
                onToggleLyrics = {
                    showInlineLyrics = !showInlineLyrics
                },
            )
        }
    }
}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle(initialValue = -1)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    var appInForeground by remember {
        mutableStateOf(
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )
    }
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer =
            LifecycleEventObserver { _, _ ->
                appInForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val nextMetadata =
        remember(queueWindows, currentWindowIndex) {
            if (currentWindowIndex >= 0 && currentWindowIndex + 1 < queueWindows.size) {
                queueWindows[currentWindowIndex + 1].mediaItem.metadata
            } else {
                null
            }
        }

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint =
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.metrolist.music.di.LyricsHelperEntryPoint::class.java,
                        )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    // Prefetch lyrics for the next queue item only while the lyrics pane is visible, the app is in the
    // foreground, and the current track's lyrics row has finished loading (avoids competing with the
    // active fetch).
    LaunchedEffect(
        nextMetadata?.id,
        showLyrics,
        appInForeground,
        mediaMetadata?.id,
        currentLyrics,
    ) {
        if (!showLyrics || !appInForeground || nextMetadata == null) return@LaunchedEffect
        val loadedForCurrent =
            currentLyrics?.let { lyrics ->
                mediaMetadata == null || lyrics.id == mediaMetadata.id
            } == true
        if (mediaMetadata != null && !loadedForCurrent) return@LaunchedEffect
        val nextId = nextMetadata.id
        delay(400)
        if (!showLyrics || !appInForeground || !isActive) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val existing = database.lyrics(nextId).first()
                if (existing != null) return@withContext
                val entryPoint =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.metrolist.music.di.LyricsHelperEntryPoint::class.java,
                    )
                val lyricsHelper = entryPoint.lyricsHelper()
                val fetched = lyricsHelper.getLyrics(nextMetadata)
                database.query {
                    upsert(LyricsEntity(nextId, fetched.lyrics, fetched.provider))
                }
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            lyrics == null -> {
                ContainedLoadingIndicator()
            }

            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    Lyrics(
                        sliderPositionProvider = positionProvider,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        showLyrics = showLyrics,
                    )
                }
                ProvideTextStyle(
                    value =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        ),
                ) {
                    lyricsContent()
                }
            }
        }
    }
}
