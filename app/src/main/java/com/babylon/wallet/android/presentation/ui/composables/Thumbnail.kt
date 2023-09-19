package com.babylon.wallet.android.presentation.ui.composables

import android.net.Uri
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
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.Badge
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.ValidatorDetail
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import rdx.works.core.toEncodedString
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal
import kotlin.math.absoluteValue

object Thumbnail {

    @Composable
    fun Fungible(
        modifier: Modifier = Modifier,
        token: Resource.FungibleResource,
    ) {
        var viewSize: IntSize? by remember { mutableStateOf(null) }

        val imageType = remember(token, viewSize) {
            val size = viewSize
            if (token.isXrd) {
                ImageType.InternalRes(drawableRes = R.drawable.ic_xrd_token)
            } else if (size != null) {
                val icon = token.iconUrl
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
            contentDescription = token.displayTitle
        )
    }

    @Composable
    fun NonFungible(
        modifier: Modifier = Modifier,
        collection: Resource.NonFungibleResource,
        shape: Shape
    ) {
        var viewSize: IntSize? by remember { mutableStateOf(null) }
        val imageType = remember(collection, viewSize) {
            val icon = collection.iconUrl
            val size = viewSize
            if (icon != null && size != null) {
                ImageType.External(icon, ThumbnailRequestSize.closest(size))
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
            shape = shape,
            contentDescription = collection.name
        )
    }

    @Composable
    fun NFT(
        modifier: Modifier = Modifier,
        nft: Resource.NonFungibleResource.Item,
        cropped: Boolean = true, // When false the NFT will appear in full height
        cornerRadius: Dp = 12.dp,
        maxAspectRatio: Float = 16f / 9f
    ) {
        val image = nft.imageUrl
        if (image != null) {
            val context = LocalContext.current
            val request = remember(image) {
                val imageType = ImageType.External(image, ThumbnailRequestSize.LARGE)

                ImageRequest.Builder(context)
                    .data(imageType.cloudFlareUri)
                    .error(R.drawable.ic_broken_image)
                    .decoderFactory(SvgDecoder.Factory())
                    // Needed for cloudflare
                    .addHeader("accept", "text/html")
                    .build()
            }

            var painterState: AsyncImagePainter.State by remember(image) { mutableStateOf(AsyncImagePainter.State.Empty) }
            val density = LocalDensity.current

            SubcomposeAsyncImage(
                modifier = modifier,
                model = request,
                contentDescription = nft.localId.displayable,
                onState = { painterState = it }
            ) {
                Image(
                    modifier = Modifier
                        .applyIf(
                            condition = cropped,
                            modifier = when (val state = painterState) {
                                is AsyncImagePainter.State.Empty -> Modifier
                                is AsyncImagePainter.State.Error -> Modifier.aspectRatio(maxAspectRatio)
                                is AsyncImagePainter.State.Loading -> Modifier
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
                        )
                        .clip(RoundedCornerShape(cornerRadius))
                        .applyIf(
                            condition = painterState !is AsyncImagePainter.State.Success,
                            modifier = Modifier.background(RadixTheme.colors.gray4)
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
        persona: Network.Persona
    ) {
        Custom(
            modifier = modifier,
            imageType = null, // We don't support persona avatars yet
            emptyDrawable = R.drawable.ic_persona,
            shape = CircleShape,
            contentDescription = persona.displayName
        )
    }

    @Composable
    fun Badge(
        modifier: Modifier = Modifier,
        badge: Badge
    ) {
        Custom(
            modifier = modifier,
            imageType = badge.icon?.let { ImageType.External(it, ThumbnailRequestSize.SMALL) },
            emptyDrawable = R.drawable.ic_badge,
            shape = RadixTheme.shapes.roundedRectXSmall,
            contentDescription = badge.name.orEmpty()
        )
    }

    @Composable
    fun DApp(
        modifier: Modifier = Modifier,
        dapp: DAppWithMetadata?,
        shape: Shape = RadixTheme.shapes.roundedRectDefault
    ) {
        Custom(
            modifier = modifier,
            imageType = dapp?.iconUrl?.let { ImageType.External(it, ThumbnailRequestSize.MEDIUM) },
            emptyDrawable = R.drawable.ic_dapp,
            shape = shape,
            contentDescription = dapp?.name.orEmpty()
        )
    }

    @Composable
    fun LSU(
        modifier: Modifier = Modifier,
        liquidStakeUnit: Resource.LiquidStakeUnitResource
    ) {
        Custom(
            modifier = modifier,
            imageType = liquidStakeUnit.fungibleResource.iconUrl?.let { ImageType.External(it, ThumbnailRequestSize.LARGE) },
            emptyDrawable = R.drawable.ic_pool_units,
            emptyContentScale = CustomContentScale.standard(density = LocalDensity.current),
            shape = RadixTheme.shapes.roundedRectMedium,
            contentDescription = liquidStakeUnit.fungibleResource.displayTitle
        )
    }

    @Composable
    fun PoolUnit(
        modifier: Modifier = Modifier,
        poolUnit: Resource.PoolUnitResource
    ) {
        Custom(
            modifier = modifier,
            imageType = poolUnit.poolUnitResource.iconUrl?.let { ImageType.External(it, ThumbnailRequestSize.LARGE) },
            emptyDrawable = R.drawable.ic_pool_units,
            emptyContentScale = CustomContentScale.standard(density = LocalDensity.current),
            shape = RadixTheme.shapes.roundedRectMedium,
            contentDescription = poolUnit.poolUnitResource.displayTitle
        )
    }

    @Composable
    fun Validator(
        modifier: Modifier = Modifier,
        validator: ValidatorDetail
    ) {
        Custom(
            modifier = modifier,
            imageType = validator.url?.let { ImageType.External(it, ThumbnailRequestSize.MEDIUM) },
            emptyDrawable = R.drawable.ic_nfts,
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
        backgroundColor: Color = RadixTheme.colors.gray4
    ) {
        val context = LocalContext.current
        val data: Any? = when (imageType) {
            is ImageType.External -> remember(imageType) { imageType.cloudFlareUri }
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
                .decoderFactory(SvgDecoder.Factory())
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
            val cloudFlareUri: Uri
                get() = Uri.parse(
                    "${BuildConfig.IMAGE_HOST_BASE_URL}/?imageOrigin=${uri.toEncodedString()}&imageSize=${size.toSizeString()}"
                )
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

    fun toSizeString(): String {
        return "${size}x$size"
    }

    companion object {
        fun closest(from: IntSize): ThumbnailRequestSize = values().minByOrNull { (from.width - it.size).absoluteValue } ?: MEDIUM
    }
}

@Composable
@Preview(name = "Fungibles Preview")
fun FungibleResourcesPreview() {
    RadixWalletTheme {
        Column(
            modifier = Modifier
                .background(RadixTheme.colors.defaultBackground)
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "With correct url")
            Thumbnail.Fungible(
                modifier = Modifier.size(100.dp),
                token = Resource.FungibleResource(
                    resourceAddress = SampleDataProvider().randomAddress(),
                    ownedAmount = BigDecimal.ZERO,
                    iconUrlMetadataItem = IconUrlMetadataItem(
                        Uri.parse("https://upload.wikimedia.org/wikipedia/commons/thumb/4/46/Bitcoin.svg/1200px-Bitcoin.svg.png")
                    )
                )
            )

            Text(text = "With no url")
            Thumbnail.Fungible(
                modifier = Modifier.size(100.dp),
                token = Resource.FungibleResource(
                    resourceAddress = SampleDataProvider().randomAddress(),
                    ownedAmount = BigDecimal.ZERO
                )
            )

            Text(text = "With malformed image")
            Thumbnail.Fungible(
                modifier = Modifier.size(100.dp),
                token = Resource.FungibleResource(
                    resourceAddress = SampleDataProvider().randomAddress(),
                    ownedAmount = BigDecimal.ZERO,
                    iconUrlMetadataItem = IconUrlMetadataItem(Uri.parse("https://upload.wikimedia.org/wikipedia/commons/thumb/"))
                )
            )
        }
    }
}

@Composable
@Preview(name = "NonFungibles Preview")
fun NonFungibleResourcesPreview() {
    RadixWalletTheme {
        Column(
            modifier = Modifier
                .background(RadixTheme.colors.defaultBackground)
                .padding(RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "With correct url")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val withUrl = remember {
                    Resource.NonFungibleResource(
                        resourceAddress = SampleDataProvider().randomAddress(),
                        amount = 0,
                        iconMetadataItem = IconUrlMetadataItem(
                            Uri.parse("https://upload.wikimedia.org/wikipedia/commons/b/be/VeKings.png")
                        ),
                        items = emptyList()
                    )
                }

                Thumbnail.NonFungible(
                    modifier = Modifier.size(100.dp),
                    collection = withUrl,
                    shape = CircleShape
                )

                Thumbnail.NonFungible(
                    modifier = Modifier.size(100.dp),
                    collection = withUrl,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
            }

            Text(text = "With no url")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val withNoUrl = remember {
                    Resource.NonFungibleResource(
                        resourceAddress = SampleDataProvider().randomAddress(),
                        amount = 0,
                        items = emptyList()
                    )
                }

                Thumbnail.NonFungible(
                    modifier = Modifier.size(100.dp),
                    collection = withNoUrl,
                    shape = CircleShape
                )

                Thumbnail.NonFungible(
                    modifier = Modifier.size(100.dp),
                    collection = withNoUrl,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
            }

            Text(text = "With malformed image")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val error = remember {
                    Resource.NonFungibleResource(
                        resourceAddress = SampleDataProvider().randomAddress(),
                        amount = 0,
                        iconMetadataItem = IconUrlMetadataItem(Uri.parse("https://upload.wikimedia.org/wikipedia/commons/")),
                        items = emptyList()
                    )
                }

                Thumbnail.NonFungible(
                    modifier = Modifier.size(100.dp),
                    collection = error,
                    shape = CircleShape
                )

                Thumbnail.NonFungible(
                    modifier = Modifier.size(100.dp),
                    collection = error,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
            }
        }
    }
}

@Composable
@Preview(name = "NFTs Preview")
fun NFTsPreview() {
    RadixWalletTheme {
        Column(
            modifier = Modifier
                .background(RadixTheme.colors.defaultBackground)
                .padding(RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Landscape NFT")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = SampleDataProvider().randomAddress(),
                    localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                    iconMetadataItem = IconUrlMetadataItem(
                        Uri.parse("https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images/KL%20Haze-medium.jpg")
                    )
                )
            )

            Text(text = "Large NFT")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = SampleDataProvider().randomAddress(),
                    localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                    iconMetadataItem = IconUrlMetadataItem(
                        Uri.parse(
                            "https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images/" +
                                "Filling+Station+Breakfast-large.jpg"
                        )
                    )
                )
            )

            Text(text = "Tall Nft")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = SampleDataProvider().randomAddress(),
                    localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                    iconMetadataItem = IconUrlMetadataItem(
                        Uri.parse(
                            "https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images/Fried+Kway+Teow-large.jpg"
                        )
                    )
                )
            )

            Text(text = "Malformed NFT")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = SampleDataProvider().randomAddress(),
                    localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                    iconMetadataItem = IconUrlMetadataItem(
                        Uri.parse("https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images")
                    )
                )
            )

            Text(text = "Full height NFT")
            Thumbnail.NFT(
                modifier = Modifier.fillMaxWidth(),
                nft = Resource.NonFungibleResource.Item(
                    collectionAddress = SampleDataProvider().randomAddress(),
                    localId = Resource.NonFungibleResource.Item.ID.from("#1#"),
                    iconMetadataItem = IconUrlMetadataItem(
                        Uri.parse(
                            "https://image-service-test-images.s3.eu-west-2.amazonaws.com/wallet_test_images/Fried+Kway+Teow-large.jpg"
                        )
                    )
                ),
                cropped = false
            )
        }
    }
}
