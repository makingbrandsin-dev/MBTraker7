package com.example.milo

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.domain.milo.MiloState
import java.io.File
import java.io.FileOutputStream

/**
 * Singleton Cache Provider for ExoPlayer MP4 Video Streaming.
 * Uses Media3 SimpleCache with LeastRecentlyUsedCacheEvictor (100MB limit)
 * to locally store streamed Milo status animation MP4s for instant, smooth playback.
 */
@OptIn(UnstableApi::class)
object MiloVideoCache {
    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.applicationContext.cacheDir, "milo_video_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB Cache Limit
            val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    @Synchronized
    fun createCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getCache(context)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val upstreamFactory = DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}

object MiloVideoHelper {

    /**
     * Resolves the best available Uri for Milo status animation:
     * 1. Internal stored File uploaded by user/admin
     * 2. Local res/raw MP4 file
     * 3. Configured custom remote URL in SharedPreferences
     */
    fun getMiloVideoUri(context: Context, state: MiloState): Uri? {
        val file = getMiloVideoFile(context, state)
        if (file != null) return Uri.fromFile(file)

        val resId = getMiloVideoResId(context, state)
        if (resId != null) return Uri.parse("android.resource://${context.packageName}/$resId")

        val savedUrl = getMiloVideoUrl(context, state)
        if (!savedUrl.isNullOrBlank()) return Uri.parse(savedUrl)

        return null
    }

    /**
     * Looks up any MP4 resource placed in res/raw by filename convention:
     * 1. Specific state: milo_idle.mp4, milo_thinking.mp4, milo_working.mp4, etc.
     * 2. General fallback: milo_status.mp4, milo_video.mp4, milo_splash.mp4
     */
    fun getMiloVideoResId(context: Context, state: MiloState): Int? {
        val stateName = when (state) {
            MiloState.IDLE -> "milo_idle"
            MiloState.THINKING -> "milo_thinking"
            MiloState.CELEBRATION -> "milo_celebrating"
            MiloState.CONVERTED -> "milo_celebrating"
            MiloState.WORKING -> "milo_working"
            MiloState.WELCOME -> "milo_welcome"
            MiloState.GOODBYE -> "milo_sleeping"
            else -> "milo_${state.name.lowercase()}"
        }
        val stateResId = context.resources.getIdentifier(stateName, "raw", context.packageName)
        if (stateResId != 0) return stateResId

        val idleResId = context.resources.getIdentifier("milo_idle", "raw", context.packageName)
        if (idleResId != 0) return idleResId

        val generalResId = context.resources.getIdentifier("milo_status", "raw", context.packageName)
        if (generalResId != 0) return generalResId

        val defaultVideoResId = context.resources.getIdentifier("milo_video", "raw", context.packageName)
        if (defaultVideoResId != 0) return defaultVideoResId

        return null
    }

    /**
     * Checks if a custom MP4 file exists in app internal storage.
     */
    fun getMiloVideoFile(context: Context, state: MiloState): File? {
        val dir = File(context.filesDir, "milo")
        if (!dir.exists()) return null

        val stateFile = File(dir, "milo_${state.name.lowercase()}.mp4")
        if (stateFile.exists() && stateFile.length() > 0) return stateFile

        val generalFile = File(dir, "milo_status.mp4")
        if (generalFile.exists() && generalFile.length() > 0) return generalFile

        return null
    }

    /**
     * Gets custom saved remote video URL from preferences.
     */
    fun getMiloVideoUrl(context: Context, state: MiloState? = null): String? {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        val key = if (state != null) "milo_url_${state.name.lowercase()}" else "milo_url_default"
        return prefs.getString(key, null) ?: prefs.getString("milo_url_default", null)
    }

    /**
     * Saves custom remote video URL to preferences.
     */
    fun saveMiloVideoUrl(context: Context, url: String, state: MiloState? = null) {
        val prefs = context.getSharedPreferences("milo_video_prefs", Context.MODE_PRIVATE)
        val key = if (state != null) "milo_url_${state.name.lowercase()}" else "milo_url_default"
        prefs.edit().putString(key, url).apply()
    }

    /**
     * Checks for splash video file in res/raw or internal files.
     */
    fun getSplashVideoResId(context: Context): Int? {
        val splashId = context.resources.getIdentifier("milo_splash", "raw", context.packageName)
        if (splashId != 0) return splashId
        val statusId = context.resources.getIdentifier("milo_status", "raw", context.packageName)
        if (statusId != 0) return statusId
        return null
    }

    /**
     * Saves an uploaded MP4 file to local storage.
     */
    fun saveMiloVideo(context: Context, uri: Uri, customName: String = "milo_status.mp4"): Boolean {
        return try {
            val dir = File(context.filesDir, "milo").apply { mkdirs() }
            val destFile = File(dir, customName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Looping, muted MP4 Video Player Composable powered by ExoPlayer + CacheDataSource.
 * Streams remote or local MP4 files efficiently and caches them locally for smooth playback.
 */
@OptIn(UnstableApi::class)
@Composable
fun MiloVideoSurface(
    videoResId: Int? = null,
    videoFile: File? = null,
    videoUrl: String? = null,
    videoUri: Uri? = null,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val targetUri: Uri? = remember(videoResId, videoFile, videoUrl, videoUri) {
        when {
            videoUri != null -> videoUri
            !videoUrl.isNullOrBlank() -> Uri.parse(videoUrl)
            videoResId != null -> Uri.parse("android.resource://${context.packageName}/$videoResId")
            videoFile != null -> Uri.fromFile(videoFile)
            else -> null
        }
    }

    val isEmulator = remember {
        android.os.Build.FINGERPRINT.startsWith("generic") ||
        android.os.Build.MODEL.contains("google_sdk") ||
        android.os.Build.MODEL.contains("Emulator") ||
        android.os.Build.MODEL.contains("Android SDK built for x86") ||
        android.os.Build.HARDWARE.contains("goldfish") ||
        android.os.Build.HARDWARE.contains("ranchu") ||
        android.os.Build.PRODUCT.contains("sdk_gphone")
    }

    if (isEmulator || targetUri == null) return

    val exoPlayer = remember(appContext, targetUri) {
        try {
            val isRemote = targetUri.scheme?.startsWith("http") == true
            val mediaSource = if (isRemote) {
                val cacheDataSourceFactory = MiloVideoCache.createCacheDataSourceFactory(appContext)
                ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(targetUri))
            } else {
                val dataSourceFactory = DefaultDataSource.Factory(appContext)
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(targetUri))
            }

            ExoPlayer.Builder(appContext).build().apply {
                volume = 0f // Silent loop for status animation
                repeatMode = Player.REPEAT_MODE_ALL // Infinite seamless loop
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.w("MiloVideoSurface", "Video playback warning safely handled: ${error.message}")
                    }
                })
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
        } catch (e: Exception) {
            android.util.Log.e("MiloVideoSurface", "Error initializing ExoPlayer", e)
            null
        }
    }

    if (exoPlayer == null) return

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                playerViewRef?.player = null
                playerViewRef = null
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val pv = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.milo_texture_player_view, null) as PlayerView
            pv.apply {
                this.resizeMode = resizeMode
                keepScreenOn = false
                player = exoPlayer
                playerViewRef = this
            }
        },
        update = { playerView ->
            playerViewRef = playerView
            if (playerView.player != exoPlayer) {
                playerView.player = exoPlayer
            }
        }
    )
}

/**
 * Real Milo Status Composable with ExoPlayer caching data source support.
 * Automatically switches to MP4 video when MP4 file or streaming URL is present,
 * or gracefully renders the high-fidelity real Milo Mascot photo with smooth breathing aura.
 */
@Composable
fun MiloRealStatusView(
    state: MiloState,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showStateBadge: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Infinite breathing motion
    val infiniteTransition = rememberInfiniteTransition(label = "MiloRealMotion")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    // Check for MP4 videoUri (local file, raw resource, or cached remote URL)
    val videoUri = remember(state) { MiloVideoHelper.getMiloVideoUri(context, state) }
    val hasMp4 = videoUri != null

    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Glowing status ambient aura behind Milo
        Surface(
            shape = CircleShape,
            color = state.primaryColor.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxSize()
                .scale(breathingScale)
        ) {}

        // Main Character / Video Body
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // Real Milo Lion Mascot Photo Asset always as base layer
            val imageRes = if (state == MiloState.THINKING) {
                R.drawable.milo_thinking
            } else {
                R.drawable.milo_final
            }

            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Milo Real Status - ${state.title}",
                modifier = Modifier
                    .fillMaxSize()
                    .scale(breathingScale)
            )

            if (hasMp4 && videoUri != null) {
                // Play Real MP4 Video Loop via ExoPlayer + TextureView on top
                MiloVideoSurface(
                    videoUri = videoUri,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Clean Modern State Emoji Badge (Bottom-Right)
        if (showStateBadge) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (size > 80.dp) 26.dp else 22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = state.emoji,
                        fontSize = if (size > 80.dp) 13.sp else 11.sp
                    )
                }
            }
        }
    }
}
