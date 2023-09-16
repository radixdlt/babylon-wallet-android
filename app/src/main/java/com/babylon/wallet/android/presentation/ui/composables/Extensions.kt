package com.babylon.wallet.android.presentation.ui.composables

import android.content.Context
import android.net.Uri
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
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.behaviours.ResourceBehaviour
import rdx.works.core.toEncodedString

// For some reason Coil library requires this header to be added when using with cloudflare service. Otherwise it fails
private fun Context.buildImageRequest(
    imageUrl: Uri?,
    @DrawableRes placeholder: Int?,
    @DrawableRes error: Int?
): ImageRequest {
    return ImageRequest.Builder(this).data(imageUrl).apply {
        placeholder?.let {
            this.placeholder(it)
        } ?: this
    }.apply {
        error?.let {
            this.error(it)
        } ?: this
    }.decoderFactory(SvgDecoder.Factory()).addHeader("accept", "text/html").build()
}

@Composable
fun rememberImageUrl(
    fromUrl: Uri?,
    size: ThumbnailRequestSize = ThumbnailRequestSize.SMALL,
    @DrawableRes placeholder: Int? = null,
    @DrawableRes error: Int? = null
): ImageRequest {
    val context = LocalContext.current
    return remember(fromUrl, size, placeholder, error) {
        val requestUrl = fromUrl?.let {
            Uri.parse(
                "${BuildConfig.IMAGE_HOST_BASE_URL}/?imageOrigin=${it.toEncodedString()}&imageSize=${size.toSizeString()}"
            )
        }

        context.buildImageRequest(requestUrl, placeholder, error)
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
fun ResourceBehaviour.name(isXrd: Boolean = false): String {
    return when (this) {
        ResourceBehaviour.PERFORM_MINT_BURN -> stringResource(
            id = if (isXrd) R.string.accountSettings_behaviors_supplyFlexibleXrd else title
        )

        else -> stringResource(id = title)
    }
}

@Composable
fun ResourceBehaviour.icon(): Painter = painterResource(id = icon)
