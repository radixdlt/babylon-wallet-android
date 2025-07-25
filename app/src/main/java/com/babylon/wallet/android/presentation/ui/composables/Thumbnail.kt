package com.babylon.wallet.android.presentation.ui.composables

import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.decode.DecodeUtils
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.isAnimatedHeif
import coil.decode.isAnimatedWebP
import coil.decode.isGif
import coil.decode.isSvg
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.Url
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.intoImageUrl
import com.radixdlt.sargon.extensions.toUrl
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.DApp
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Pool
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.metadata.iconUrl
import rdx.works.core.domain.resources.metadata.name
import rdx.works.core.domain.resources.sampleMainnet
import kotlin.math.absoluteValue

@Suppress("TooManyFunctions")
object Thumbnail {

    @Composable
    fun Fungible(
        modifier: Modifier = Modifier,
        token: Resource.FungibleResource,
    ) {
        Fungible(
            modifier = modifier,
            isXrd = token.isXrd,
            icon = token.iconUrl,
            name = token.name
        )
    }

    @Composable
    fun Fungible(
        modifier: Modifier = Modifier,
        isXrd: Boolean,
        icon: Uri?,
        name: String
    ) {
        var viewSize: IntSize? by remember { mutableStateOf(null) }

        val imageType = remember(isXrd, icon, viewSize) {
            val size = viewSize
            if (isXrd) {
                ImageType.InternalRes(drawableRes = R.drawable.ic_xrd_token)
            } else if (size != null) {
                if (icon != null) {
                    ImageType.External(
                        uri = icon,
                        size = ThumbnailRequestSize.closest(size)
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
        Custom(
            modifier = modifier.onGloballyPositioned { viewSize = it.size },
            imageType = imageType,
            imageContentScale = ContentScale.Crop,
            emptyDrawable = R.drawable.ic_token,
            shape = CircleShape,
            contentDescription = name
        )
    }

    @Composable
    fun NonFungible(
        modifier: Modifier = Modifier,
        image: Uri?,
        name: String? = null,
        radius: CornerSize = CornerSize(size = RadixTheme.dimensions.paddingSmall)
    ) {
        var viewSize: IntSize? by remember { mutableStateOf(null) }
        val imageType = remember(image, viewSize) {
            val size = viewSize
            if (image != null && size != null) {
                ImageType.External(image, ThumbnailRequestSize.closest(size))
            } else {
                null
            }
        }
        val density = LocalDensity.current
        Custom(
            modifier = modifier.onGloballyPositioned { viewSize = it.size },
            imageType = imageType,
            imageContentScale = ContentScale.Crop,
            emptyDrawable = R.drawable.ic_nfts,
            emptyContentScale = CustomContentScale.standard(density = density),
            shape = RoundedCornerShape(radius),
            contentDescription = name.orEmpty()
        )
    }

    @Composable
    fun NonFungible(
        modifier: Modifier = Modifier,
        collection: Resource.NonFungibleResource?,
        radius: CornerSize = CornerSize(size = RadixTheme.dimensions.paddingSmall)
    ) {
        NonFungible(
            modifier = modifier,
            image = collection?.iconUrl,
            name = collection?.name,
            radius = radius
        )
    }

    val NFTCornerRadius = 12.dp
    const val NFTAspectRatio = 16f / 9f

    @Composable
    fun NFT(
        modifier: Modifier = Modifier,
        nft: Resource.NonFungibleResource.Item,
        cropped: Boolean = true, // When false the NFT will appear in full height
        cornerRadius: Dp = NFTCornerRadius,
        maxAspectRatio: Float = NFTAspectRatio
    ) {
        NFT(
            modifier = modifier,
            image = nft.imageUrl,
            localId = nft.localId.formatted(),
            cropped = cropped,
            cornerRadius = cornerRadius,
            maxAspectRatio = maxAspectRatio
        )
    }

    @Composable
    fun NFT(
        modifier: Modifier = Modifier,
        image: Uri?,
        localId: String?,
        cropped: Boolean = true, // When false the NFT will appear in full height
        cornerRadius: Dp = NFTCornerRadius,
        maxAspectRatio: Float = NFTAspectRatio
    ) {
        if (image != null) {
            val context = LocalContext.current
            val request = remember(image) {
                val imageType = ImageType.External(image, ThumbnailRequestSize.LARGE)

                ImageRequest.Builder(context)
                    .data(imageType.cloudFlareUrl)
                    .error(R.drawable.ic_broken_image)
                    .applyCorrectDecoderBasedOnMimeType()
                    // Needed for cloudflare
                    .addHeader("accept", "text/html")
                    .build()
            }

            var painterState: AsyncImagePainter.State by remember(image) { mutableStateOf(AsyncImagePainter.State.Empty) }
            val density = LocalDensity.current
            SubcomposeAsyncImage(
                modifier = modifier,
                model = request,
                contentDescription = localId,
                onState = { painterState = it }
            ) {
                Image(
                    modifier = Modifier
                        .clip(RoundedCornerShape(cornerRadius))
                        .applyIf(
                            condition = painterState !is AsyncImagePainter.State.Success,
                            modifier = Modifier.background(RadixTheme.colors.backgroundTertiary)
                        )
                        .applyIf(
                            condition = cropped,
                            modifier = when (val state = painterState) {
                                is AsyncImagePainter.State.Empty -> Modifier
                                is AsyncImagePainter.State.Error -> Modifier.aspectRatio(maxAspectRatio)
                                is AsyncImagePainter.State.Loading ->
                                    Modifier
                                        .aspectRatio(NFTAspectRatio)
                                        .radixPlaceholder(
                                            visible = true,
                                            shape = RoundedCornerShape(NFTCornerRadius)
                                        )

                                is AsyncImagePainter.State.Success -> {
                                    val intrinsicSize = state.painter.intrinsicSize
                                    if (intrinsicSize.height > intrinsicSize.width) {
                                        // If the image is taller in aspect ratio than a square,
                                        // crop the image to the largest possible centered square
                                        Modifier.aspectRatio(1f)
                                    } else if (intrinsicSize.width / intrinsicSize.height > maxAspectRatio) {
                                        // If the image is wider in aspect ratio than maxAspectRatio,
                                        // crop the image to the largest possible centered maxAspectRatio rectangle
                                        Modifier.aspectRatio(maxAspectRatio)
                                    } else {
                                        Modifier
                                    }
                                }
                            }
                        )
                        .applyIf(
                            condition = !cropped,
                            modifier = when (painterState) {
                                is AsyncImagePainter.State.Error -> Modifier.aspectRatio(maxAspectRatio)
                                else -> Modifier.wrapContentHeight()
                            }
                        ),
                    painter = painter,
                    contentDescription = null,
                    contentScale = when (painterState) {
                        is AsyncImagePainter.State.Error -> CustomContentScale.standard(density)
                        else -> if (cropped) ContentScale.Crop else ContentScale.FillWidth
                    }
                )
            }
        }
    }

    @Composable
    fun Persona(
        modifier: Modifier = Modifier,
        persona: Persona?
    ) {
        Custom(
            modifier = modifier,
            imageType = null, // We don't support persona avatars yet
            emptyDrawable = R.drawable.ic_persona,
            shape = CircleShape,
            contentDescription = persona?.displayName?.value.orEmpty()
        )
    }

    @Composable
    fun Badge(
        modifier: Modifier = Modifier,
        badge: Badge
    ) {
        when (val resource = badge.resource) {
            is Resource.FungibleResource -> Fungible(
                modifier = modifier,
                token = resource
            )

            is Resource.NonFungibleResource -> NonFungible(
                modifier = modifier,
                collection = resource
            )
        }
    }

    @Composable
    fun DApp(
        modifier: Modifier = Modifier,
        dapp: DApp?,
        shape: Shape = RadixTheme.shapes.roundedRectDefault,
        backgroundColor: Color = RadixTheme.colors.backgroundTertiary
    ) {
        DApp(
            modifier = modifier,
            dAppIconUrl = dapp?.iconUrl,
            dAppName = dapp?.name.orEmpty(),
            shape = shape,
            backgroundColor = backgroundColor
        )
    }

    @Composable
    fun DApp(
        modifier: Modifier = Modifier,
        dAppIconUrl: Uri?,
        dAppName: String,
        shape: Shape = RadixTheme.shapes.roundedRectDefault,
        backgroundColor: Color = RadixTheme.colors.backgroundTertiary
    ) {
        Custom(
            modifier = modifier,
            imageType = dAppIconUrl?.let { ImageType.External(it, ThumbnailRequestSize.MEDIUM) },
            emptyDrawable = R.drawable.ic_dapp,
            shape = shape,
            contentDescription = dAppName,
            backgroundColor = backgroundColor
        )
    }

    @Composable
    fun LSU(
        modifier: Modifier = Modifier,
        liquidStakeUnit: LiquidStakeUnit?
    ) {
        LSU(
            modifier = modifier,
            iconUrl = liquidStakeUnit?.fungibleResource?.iconUrl,
            name = liquidStakeUnit?.fungibleResource?.name
        )
    }

    @Composable
    fun LSU(
        modifier: Modifier = Modifier,
        iconUrl: Uri?,
        name: String?
    ) {
        Custom(
            modifier = modifier,
            imageType = iconUrl?.let { ImageType.External(it, ThumbnailRequestSize.LARGE) },
            emptyDrawable = DSR.ic_lsu,
            emptyContentScale = CustomContentScale.standard(density = LocalDensity.current),
            shape = RadixTheme.shapes.roundedRectMedium,
            contentDescription = name.orEmpty()
        )
    }

    @Composable
    fun PoolUnit(
        modifier: Modifier = Modifier,
        poolUnit: PoolUnit
    ) {
        PoolUnit(
            modifier = modifier,
            iconUrl = poolUnit.stake.iconUrl,
            name = poolUnit.stake.name
        )
    }

    @Composable
    fun PoolUnit(
        modifier: Modifier = Modifier,
        iconUrl: Uri?,
        name: String
    ) {
        Custom(
            modifier = modifier,
            imageType = iconUrl?.let { ImageType.External(it, ThumbnailRequestSize.LARGE) },
            emptyDrawable = R.drawable.ic_pool_units,
            emptyContentScale = CustomContentScale.standard(density = LocalDensity.current),
            shape = CircleShape,
            contentDescription = name
        )
    }

    @Composable
    fun Pool(
        modifier: Modifier = Modifier,
        pool: Pool
    ) {
        Custom(
            modifier = modifier,
            imageType = pool.metadata.iconUrl()?.let { ImageType.External(it, ThumbnailRequestSize.MEDIUM) },
            emptyDrawable = R.drawable.ic_pool_units, // TODO change with proper icon
            emptyContentScale = CustomContentScale.standard(density = LocalDensity.current),
            shape = RadixTheme.shapes.roundedRectMedium,
            contentDescription = pool.metadata.name().orEmpty()
        )
    }

    @Composable
    fun Validator(
        modifier: Modifier = Modifier,
        validator: Validator
    ) {
        Custom(
            modifier = modifier,
            imageType = validator.url?.let { ImageType.External(it, ThumbnailRequestSize.MEDIUM) },
            emptyDrawable = DSR.ic_validator,
            emptyContentScale = CustomContentScale.standard(density = LocalDensity.current),
            shape = RadixTheme.shapes.roundedRectSmall,
            contentDescription = validator.name
        )
    }

    @Composable
    private fun Custom(
        modifier: Modifier,
        imageType: ImageType?,
        imageContentScale: ContentScale = ContentScale.Crop,
        @DrawableRes emptyDrawable: Int? = null,
        emptyContentScale: ContentScale = ContentScale.FillBounds,
        @DrawableRes errorDrawable: Int? = R.drawable.ic_broken_image,
        errorContentScale: CustomContentScale = CustomContentScale.standard(LocalDensity.current),
        shape: Shape,
        contentDescription: String,
        backgroundColor: Color = RadixTheme.colors.backgroundTertiary
    ) {
        val context = LocalContext.current
        val data: Any? = when (imageType) {
            is ImageType.External -> remember(imageType) { imageType.cloudFlareUrl }
            is ImageType.InternalRes -> imageType.drawableRes
            else -> null
        }

        val imageRequest = remember(data, emptyDrawable, errorDrawable) {
            ImageRequest.Builder(context)
                .data(data)
                .apply {
                    if (emptyDrawable != null) {
                        fallback(emptyDrawable)
                    }

                    if (errorDrawable != null) {
                        error(errorDrawable)
                    }
                }
                .applyCorrectDecoderBasedOnMimeType()
                // Needed for cloudflare
                .addHeader("accept", "text/html")
                .build()
        }

        var painterState: AsyncImagePainter.State by remember(data) { mutableStateOf(AsyncImagePainter.State.Empty) }

        AsyncImage(
            modifier = modifier
                .clip(shape)
                .applyIf(
                    condition = painterState !is AsyncImagePainter.State.Success,
                    modifier = Modifier.background(backgroundColor)
                ),
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = when (val state = painterState) {
                is AsyncImagePainter.State.Error -> {
                    if (state.result.throwable is NullRequestDataException) {
                        // Url was null, placeholder visible
                        emptyContentScale
                    } else {
                        // Error making the request, error visible
                        errorContentScale
                    }
                }

                else -> imageContentScale
            },
            onState = { state ->
                painterState = state
            }
        )
    }

    sealed interface ImageType {
        data class External(
            private val uri: Uri,
            private val size: ThumbnailRequestSize
        ) : ImageType {
            val cloudFlareUrl: Url
                get() = uri.intoImageUrl(
                    imageServiceUrl = imageServiceUrl,
                    size = size.toSize()
                )

            companion object {
                private val imageServiceUrl = BuildConfig.IMAGE_HOST_BASE_URL.toUrl()
            }
        }

        data class InternalRes(@DrawableRes val drawableRes: Int) : ImageType
    }

    private class CustomContentScale(private val sizeRange: IntRange) : ContentScale {
        override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
            val optimalDimensionSize = dstSize.div(2f).maxDimension.coerceIn(
                minimumValue = sizeRange.first.toFloat(),
                maximumValue = sizeRange.last.toFloat()
            )

            val scale = optimalDimensionSize / srcSize.maxDimension
            return ScaleFactor(scaleX = scale, scaleY = scale)
        }

        companion object {
            fun standard(density: Density): CustomContentScale = from(8.dp, 40.dp, density)

            fun from(minSize: Dp, maxSize: Dp, density: Density): CustomContentScale {
                val min = with(density) { minSize.toPx().toInt() }
                val max = with(density) { maxSize.toPx().toInt() }
                return CustomContentScale(IntRange(start = min, endInclusive = max))
            }
        }
    }
}

@Suppress("MagicNumber")
enum class ThumbnailRequestSize(val size: Int) {
    SMALL(112), MEDIUM(256), LARGE(512);

    fun toSize(): android.util.Size = android.util.Size(size, size)

    companion object {
        fun closest(from: IntSize): ThumbnailRequestSize = values().minByOrNull { (from.width - it.size).absoluteValue } ?: MEDIUM
    }
}

private fun ImageRequest.Builder.applyCorrectDecoderBasedOnMimeType() = apply {
    this.decoderFactory { result, options, _ ->
        if (result.mimeType == "image/svg+xml" || DecodeUtils.isSvg(result.source.source())) {
            SvgDecoder(result.source, options)
        } else if (DecodeUtils.isGif(result.source.source())) {
            GifDecoder(result.source, options)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && (
                DecodeUtils.isAnimatedWebP(result.source.source()) || DecodeUtils.isAnimatedHeif(
                    result.source.source()
                )
                )
        ) {
            ImageDecoderDecoder(result.source, options)
        } else {
            BitmapFactoryDecoder(result.source, options)
        }
    }
}

@UsesSampleValues
@Composable
@Preview(name = "Fungibles Preview")
fun FungibleResourcesPreview() {
    RadixWalletPreviewTheme {
        Column(
            modifier = Modifier
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "With correct url")
            Thumbnail.Fungible(
                modifier = Modifier.size(100.dp),
                token = Resource.FungibleResource.sampleMainnet().let {
                    it.copy(
                        metadata = it.metadata.toMutableList().apply {
                            add(
                                Metadata.Primitive(
                                    key = ExplicitMetadataKey.ICON_URL.key,
                                    value = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/46/Bitcoin.svg/1200px-Bitcoin.svg.png",
                                    valueType = MetadataType.Url
                                )
                            )
                        }
                    )
                }
            )

            Text(text = "With no url")
            Thumbnail.Fungible(
                modifier = Modifier.size(100.dp),
                token = Resource.FungibleResource.sampleMainnet.other()
            )

            Text(text = "With malformed image")
            Thumbnail.Fungible(
                modifier = Modifier.size(100.dp),
                token = Resource.FungibleResource.sampleMainnet().let {
                    it.copy(
                        metadata = it.metadata.toMutableList().apply {
                            add(
                                Metadata.Primitive(
                                    key = ExplicitMetadataKey.ICON_URL.key,
                                    value = "https://upload.wikimedia.org/wikipedia/commons/thumb/",
                                    valueType = MetadataType.Url
                                )
                            )
                        }
                    )
                }
            )
        }
    }
}

@UsesSampleValues
@Composable
@Preview(name = "NonFungibles Preview")
fun NonFungibleResourcesPreview() {
    RadixWalletPreviewTheme {
        Column(
            modifier = Modifier
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "With correct url")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val withUrl = remember {
                    Resource.NonFungibleResource.sampleMainnet().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.ICON_URL.key,
                                        value = "https://upload.wikimedia.org/wikipedia/commons/b/be/VeKings.png",
                                        valueType = MetadataType.Url
                                    )
                                )
                            }
                        )
                    }
                }

                Thumbnail.NonFungible(
                    modifier = Modifier.size(100.dp),
                    collection = withUrl,
                    radius = CornerSize(size = 12.dp)
                )

                Thumbnail.NonFungible(
                    modifier = Modifier.size(50.dp),
                    collection = withUrl
                )
            }

            Text(text = "With no url")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val withNoUrl = remember {
                    Resource.NonFungibleResource.sampleMainnet()
                }

                Thumbnail.NonFungible(
                    modifier = Modifier.size(100.dp),
                    collection = withNoUrl,
                    radius = CornerSize(size = 12.dp)
                )

                Thumbnail.NonFungible(
                    modifier = Modifier.size(50.dp),
                    collection = withNoUrl
                )
            }

            Text(text = "With malformed image")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val error = remember {
                    Resource.NonFungibleResource.sampleMainnet().let {
                        it.copy(
                            metadata = it.metadata.toMutableList().apply {
                                add(
                                    Metadata.Primitive(
                                        key = ExplicitMetadataKey.ICON_URL.key,
                                        value = "https://upload.wikimedia.org/wikipedia/commons/",
                                        valueType = MetadataType.Url
                                    )
                                )
                            }
                        )
                    }
                }

                Thumbnail.NonFungible(
                    modifier = Modifier.size(100.dp),
                    collection = error,
                    radius = CornerSize(size = 12.dp)
                )

                Thumbnail.NonFungible(
                    modifier = Modifier.size(50.dp),
                    collection = error
                )
            }
        }
    }
}

@UsesSampleValues
@Composable
@Preview(name = "NFTs Preview")
fun NFTsPreview() {
    RadixWalletPreviewTheme {
        Column(
            modifier = Modifier
                .padding(RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Landscape NFT")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = ResourceAddress.sampleMainnet.random(),
                    localId = NonFungibleLocalId.sample(),
                    metadata = listOf(
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.KEY_IMAGE_URL.key,
                            "https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images/KL%20Haze-medium.jpg",
                            valueType = MetadataType.Url
                        )
                    )
                )
            )

            Text(text = "Large NFT")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = ResourceAddress.sampleMainnet.random(),
                    localId = NonFungibleLocalId.sample(),
                    metadata = listOf(
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.KEY_IMAGE_URL.key,
                            "https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images/" +
                                "Filling+Station+Breakfast-large.jpg",
                            valueType = MetadataType.Url
                        )
                    )
                )
            )

            Text(text = "Tall Nft")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = ResourceAddress.sampleMainnet.random(),
                    localId = NonFungibleLocalId.sample(),
                    metadata = listOf(
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.KEY_IMAGE_URL.key,
                            value = "https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images/" +
                                "Fried+Kway+Teow-large.jpg",
                            valueType = MetadataType.Url
                        )
                    )
                )
            )

            Text(text = "Malformed NFT")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = ResourceAddress.sampleMainnet.random(),
                    localId = NonFungibleLocalId.sample(),
                    metadata = listOf(
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.KEY_IMAGE_URL.key,
                            value = "https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images",
                            valueType = MetadataType.Url
                        )
                    )
                )
            )

            Text(text = "Full height NFT")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = ResourceAddress.sampleMainnet.random(),
                    localId = NonFungibleLocalId.sample(),
                    metadata = listOf(
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.KEY_IMAGE_URL.key,
                            value = "https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images/" +
                                "Fried+Kway+Teow-large.jpg",
                            valueType = MetadataType.Url
                        )
                    )
                ),
                cropped = false
            )
        }
    }
}
