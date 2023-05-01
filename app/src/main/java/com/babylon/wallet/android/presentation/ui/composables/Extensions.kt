package com.babylon.wallet.android.presentation.ui.composables

import android.content.Context
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.babylon.wallet.android.BuildConfig

private const val IMAGE_RATIO = 9 / 16f

fun Modifier.applyImageAspectRatio(
    painter: AsyncImagePainter
): Modifier {
    return then(
        (painter.state as? AsyncImagePainter.State.Success)
            ?.painter?.intrinsicSize?.let { intrinsicSize ->
                if (intrinsicSize.width / intrinsicSize.height < IMAGE_RATIO) {
                    Modifier.aspectRatio(IMAGE_RATIO)
                } else {
                    Modifier
                }
            } ?: Modifier
    )
}

// For some reason Coil library requires this header to be added when using with cloudflare service. Otherwise it fails
private fun Context.buildImageRequest(imageUrl: String?): ImageRequest {
    return ImageRequest.Builder(this)
        .data(imageUrl)
        .decoderFactory(SvgDecoder.Factory())
        .addHeader("accept", "text/html")
        .build()
}

@Composable
fun rememberImageUrl(fromUrl: String?, size: ImageSize = ImageSize.SMALL): ImageRequest {
    val context = LocalContext.current
    val url = "${BuildConfig.IMAGE_HOST_BASE_URL}/?imageOrigin=$fromUrl&imageSize=${size.toSizeString()}"
    return remember(url) {
        context.buildImageRequest(url)
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
