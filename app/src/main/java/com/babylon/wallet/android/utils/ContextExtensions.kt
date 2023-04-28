package com.babylon.wallet.android.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.babylon.wallet.android.BuildConfig

fun Context.findFragmentActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

inline fun Modifier.throttleClickable(
    thresholdMs: Long = 500L,
    crossinline onClick: () -> Unit
): Modifier {
    return composed {
        var lastClickMs by remember { mutableStateOf(0L) }
        clickable {
            val now = System.currentTimeMillis()
            if (now - lastClickMs > thresholdMs) {
                onClick()
                lastClickMs = now
            }
        }
    }
}

// For some reason Coil library requires this header to be added when using with cloudflare service. Otherwise it fails
private fun Context.buildImageRequest(imageUrl: String?): ImageRequest {
    return ImageRequest.Builder(this)
        .data(imageUrl)
        .decoderFactory(SvgDecoder.Factory())
        .allowHardware(false)
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
