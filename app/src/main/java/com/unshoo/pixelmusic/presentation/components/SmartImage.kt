package com.unshoo.pixelmusic.presentation.components

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size // Import Coil's Size
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.unshoo.pixelmusic.R
import androidx.compose.runtime.collectAsState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.unshoo.pixelmusic.presentation.viewmodel.ConnectivityStateHolder
import com.unshoo.pixelmusic.data.preferences.UserPreferencesRepository
import com.unshoo.pixelmusic.data.preferences.AlbumArtQuality
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmartImageEntryPoint {
    fun connectivityStateHolder(): ConnectivityStateHolder
    fun userPreferencesRepository(): UserPreferencesRepository
}

val SmartImageCompactListTargetSize = Size(96, 96)
val SmartImageListTargetSize = Size(128, 128)
private val DefaultSmartImageSize = Size(300, 300)

@Composable
fun SmartImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderResId: Int = R.drawable.ic_music_placeholder,
    errorResId: Int = R.drawable.ic_music_placeholder,
    shape: Shape = RectangleShape,
    contentScale: ContentScale = ContentScale.Crop,
    crossfadeDurationMillis: Int = 300,
    useDiskCache: Boolean = true,
    useMemoryCache: Boolean = true,
    allowHardware: Boolean = true,
    targetSize: Size = DefaultSmartImageSize,
    colorFilter: ColorFilter? = null,
    alpha: Float = 1f,
    placeholderModel: Any? = null,
    placeHolderBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    isThumbnail: Boolean = true, // Defaults to true for all lists/explore/album/search screens
    onState: ((AsyncImagePainter.State) -> Unit)? = null
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val entryPoint = remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, SmartImageEntryPoint::class.java)
    }
    val connectivityStateHolder = entryPoint.connectivityStateHolder()
    val userPreferencesRepository = entryPoint.userPreferencesRepository()

    // Initialize the shared Compose State-backed cache once
    SmartImageCache.initialize(connectivityStateHolder, userPreferencesRepository)

    val isMeteredNetwork = SmartImageCache.isMeteredNetwork
    val albumArtQualityWifi = SmartImageCache.albumArtQualityWifi
    val albumArtQualityMobile = SmartImageCache.albumArtQualityMobile
    val performanceModeEnabled = SmartImageCache.performanceModeEnabled

    // When isThumbnail is true -> lowest quality (LOW)
    // When isThumbnail is false (Now Playing) -> respects user settings
    val effectiveQuality = if (isThumbnail || performanceModeEnabled) {
        AlbumArtQuality.LOW
    } else if (isMeteredNetwork) {
        albumArtQualityMobile
    } else {
        albumArtQualityWifi
    }

    val clippedModifier = modifier.clip(shape)
    val requestTargetSize = remember(targetSize, effectiveQuality, isThumbnail) {
        val baseSize = safeAlbumArtTargetSize(targetSize)
        val maxSize = if (isThumbnail) 120 else effectiveQuality.maxSize
        if (maxSize > 0) {
            val widthPx = (baseSize.width as? coil.size.Dimension.Pixels)?.px ?: maxSize
            val heightPx = (baseSize.height as? coil.size.Dimension.Pixels)?.px ?: maxSize
            val clampedW = if (widthPx > maxSize) maxSize else widthPx
            val clampedH = if (heightPx > maxSize) maxSize else heightPx
            Size(clampedW, clampedH)
        } else {
            coil.size.Size.ORIGINAL
        }
    }

    // Handle direct models (Bitmap, Vector, etc) early to avoid ImageRequest overhead
    if (model == null || model is ImageVector || model is Painter || model is ImageBitmap || model is Bitmap) {
        if (model == null) {
            Placeholder(
                modifier = clippedModifier,
                drawableResId = placeholderResId,
                contentDescription = contentDescription,
                containerColor = placeHolderBackgroundColor,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                alpha = alpha
            )
        } else {
            handleDirectModel(
                data = model,
                modifier = clippedModifier,
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = colorFilter,
                alpha = alpha
            )
        }
        return
    }

    // Extract raw string whether passed as String or ImageRequest
    val rawModelString = when (model) {
        is String -> model
        is ImageRequest -> model.data as? String
        else -> null
    }

    val optimizedModel = remember(model, rawModelString, effectiveQuality, isThumbnail) {
        if (rawModelString != null) {
            val targetPx = when {
                isThumbnail -> 120
                effectiveQuality.maxSize > 0 -> effectiveQuality.maxSize
                else -> 1200 // Original / Maximum Quality
            }

            var transformed = rawModelString

            // 1. Google / YouTube user content (=w1000-h1000, =s500, =w120-h120-l90-rj, etc.)
            if (transformed.contains("googleusercontent.com") || transformed.contains("ggpht.com")) {
                val sizeParamRegex = Regex("=[ws]\\d+.*")
                val slashSizeRegex = Regex("/[ws]\\d+.*")
                transformed = when {
                    sizeParamRegex.containsMatchIn(transformed) -> transformed.replace(sizeParamRegex, "=w$targetPx-h$targetPx-l90-rj")
                    slashSizeRegex.containsMatchIn(transformed) -> transformed.replace(slashSizeRegex, "/w$targetPx-h$targetPx-l90-rj")
                    transformed.contains("=") -> transformed.substringBeforeLast("=") + "=w$targetPx-h$targetPx-l90-rj"
                    else -> "$transformed=w$targetPx-h$targetPx-l90-rj"
                }
            }

            // 2. YouTube video thumbnails (maxresdefault, sddefault, hqdefault, mqdefault)
            if (transformed.contains("i.ytimg.com")) {
                val ytResolution = when {
                    isThumbnail || effectiveQuality == AlbumArtQuality.LOW -> "mqdefault"
                    effectiveQuality == AlbumArtQuality.MEDIUM -> "sddefault"
                    effectiveQuality == AlbumArtQuality.HIGH -> "hqdefault"
                    else -> "maxresdefault"
                }
                transformed = transformed.replace("maxresdefault.jpg", "$ytResolution.jpg")
                    .replace("sddefault.jpg", "$ytResolution.jpg")
                    .replace("hqdefault.jpg", "$ytResolution.jpg")
                    .replace("mqdefault.jpg", "$ytResolution.jpg")
                    .replace("maxresdefault.webp", "$ytResolution.webp")
                    .replace("sddefault.webp", "$ytResolution.webp")
                    .replace("hqdefault.webp", "$ytResolution.webp")
                    .replace("mqdefault.webp", "$ytResolution.webp")
            }

            if (model is ImageRequest) {
                model.newBuilder().data(transformed).build()
            } else {
                transformed
            }
        } else {
            model
        }
    }

    val request = remember(
        context,
        optimizedModel,
        crossfadeDurationMillis,
        useDiskCache,
        useMemoryCache,
        allowHardware,
        requestTargetSize
    ) {
        if (optimizedModel is ImageRequest) {
            optimizedModel.newBuilder(context)
                .size(requestTargetSize)
                .build()
        } else {
            ImageRequest.Builder(context)
                .data(optimizedModel)
                .memoryCacheKey("$optimizedModel|$requestTargetSize")
                .crossfade(crossfadeDurationMillis)
                .diskCachePolicy(if (useDiskCache) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .memoryCachePolicy(if (useMemoryCache) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .allowHardware(allowHardware)
                .size(requestTargetSize)
                .build()
        }
    }

    if (onState != null || placeholderModel != null) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = clippedModifier,
            contentScale = contentScale,
            colorFilter = colorFilter,
            alpha = alpha
        ) {
            val state = painter.state
            LaunchedEffect(state) {
                onState?.invoke(state)
            }

            when (state) {
                is AsyncImagePainter.State.Success -> {
                    SubcomposeAsyncImageContent()
                }
                is AsyncImagePainter.State.Loading -> {
                    if (placeholderModel != null) {
                        AsyncImage(
                            model = placeholderModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = contentScale,
                            colorFilter = colorFilter,
                            alpha = alpha
                        )
                    } else {
                        Placeholder(
                            modifier = Modifier.fillMaxSize(),
                            drawableResId = placeholderResId,
                            contentDescription = contentDescription,
                            containerColor = placeHolderBackgroundColor,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            alpha = alpha
                        )
                    }
                }
                else -> {
                    Placeholder(
                        modifier = Modifier.fillMaxSize(),
                        drawableResId = if (state is AsyncImagePainter.State.Error) errorResId else placeholderResId,
                        contentDescription = contentDescription,
                        containerColor = placeHolderBackgroundColor,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        alpha = alpha
                    )
                }
            }
        }
    } else {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = clippedModifier,
            contentScale = contentScale,
            colorFilter = colorFilter,
            alpha = alpha,
            placeholder = painterResource(placeholderResId),
            error = painterResource(errorResId)
        )
    }
}

@Composable
private fun handleDirectModel(
    data: Any?,
    modifier: Modifier,
    contentDescription: String?,
    contentScale: ContentScale,
    colorFilter: ColorFilter?,
    alpha: Float
): Any? {
    return when (data) {
        is ImageVector -> {
            Image(
                imageVector = data,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = colorFilter,
                alpha = alpha
            )
            data
        }
        is Painter -> {
            Image(
                painter = data,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = colorFilter,
                alpha = alpha
            )
            data
        }
        is ImageBitmap -> {
            Image(
                bitmap = data,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = colorFilter,
                alpha = alpha
            )
            data
        }
        is Bitmap -> {
            Image(
                bitmap = data.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                colorFilter = colorFilter,
                alpha = alpha
            )
            data
        }
        else -> null
    }
}

@Composable
private fun Placeholder(
    modifier: Modifier,
    @DrawableRes drawableResId: Int,
    contentDescription: String?,
    containerColor: Color,
    iconColor: Color,
    alpha: Float,
) {
    Box(
        modifier = modifier
            .alpha(alpha)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(drawableResId),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit
        )
    }
}

object SmartImageCache {
    var isMeteredNetwork by androidx.compose.runtime.mutableStateOf(false)
    var albumArtQualityWifi by androidx.compose.runtime.mutableStateOf(AlbumArtQuality.ORIGINAL)
    var albumArtQualityMobile by androidx.compose.runtime.mutableStateOf(AlbumArtQuality.ORIGINAL)
    var performanceModeEnabled by androidx.compose.runtime.mutableStateOf(false)

    @Volatile
    private var isInitialized = false
    private val cacheScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate
    )

    fun initialize(
        connectivityStateHolder: ConnectivityStateHolder,
        userPreferencesRepository: UserPreferencesRepository
    ) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            isInitialized = true
        }

        // Pre-warm with initial values from ConnectivityStateHolder
        isMeteredNetwork = connectivityStateHolder.isMeteredNetwork.value

        cacheScope.launch {
            connectivityStateHolder.isMeteredNetwork.collect { isMeteredNetwork = it }
        }
        cacheScope.launch {
            userPreferencesRepository.albumArtQualityFlow.collect { albumArtQualityWifi = it }
        }
        cacheScope.launch {
            userPreferencesRepository.albumArtQualityMobileFlow.collect { albumArtQualityMobile = it }
        }
        cacheScope.launch {
            userPreferencesRepository.performanceModeEnabledFlow.collect { performanceModeEnabled = it }
        }
    }
}
