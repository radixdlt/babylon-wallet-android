package com.babylon.wallet.android.presentation.ui.composables

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.behaviours.ResourceBehaviour

private const val IMAGE_RATIO = 16 / 9f

fun Modifier.applyImageAspectRatio(
    painter: AsyncImagePainter
): Modifier {
    return then(
        (painter.state as? AsyncImagePainter.State.Success)
            ?.painter?.intrinsicSize?.let { intrinsicSize ->
                // If the image is taller in aspect ratio than a square,
                // crop the image to the largest possible centered square
                if (intrinsicSize.height > intrinsicSize.width) {
                    Modifier.aspectRatio(1f)
                    // If the image is wider in aspect ratio than 16:9,
                    // crop the image to the largest possible centered 16:9 recntangle
                } else if (intrinsicSize.width / intrinsicSize.height > IMAGE_RATIO) {
                    Modifier.aspectRatio(IMAGE_RATIO)
                } else {
                    Modifier
                }
            } ?: Modifier
    )
}

// For some reason Coil library requires this header to be added when using with cloudflare service. Otherwise it fails
private fun Context.buildImageRequest(
    imageUrl: String?,
    @DrawableRes placeholder: Int?,
    @DrawableRes error: Int?
): ImageRequest {
    return ImageRequest.Builder(this)
        .data(imageUrl)
        .apply {
            placeholder?.let {
                this.placeholder(it)
            } ?: this
        }
        .apply {
            error?.let {
                this.error(it)
            } ?: this
        }
        .decoderFactory(SvgDecoder.Factory())
        .addHeader("accept", "text/html")
        .build()
}

@Composable
fun rememberImageUrl(
    fromUrl: String?,
    size: ImageSize = ImageSize.SMALL,
    @DrawableRes placeholder: Int? = null,
    @DrawableRes error: Int? = null
): ImageRequest {
    val context = LocalContext.current
    val url = "${BuildConfig.IMAGE_HOST_BASE_URL}/?imageOrigin=$fromUrl&imageSize=${size.toSizeString()}"
    return remember(url, placeholder, error) {
        context.buildImageRequest(url, placeholder, error)
    }
}

@Suppress("MagicNumber")
enum class ImageSize(val size: Int) {
    SMALL(112),
    MEDIUM(256),
    LARGE(512);

    fun toSizeString(): String {
        return "${size}x$size"
    }
}

@Composable
fun Resource.Tag.name(): String {
    return when (this) {
        is Resource.Tag.Official -> "RADIX NETWORK"
        is Resource.Tag.Dynamic -> name
    }
}

@Composable
fun ResourceBehaviour.name(): String = stringResource(id = title)

@Composable
fun ResourceBehaviour.icon(): Painter = painterResource(id = icon)
