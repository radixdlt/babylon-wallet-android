package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Increase or decrease for more or less friction when scrolling through the list
private const val SCROLL_FRICTION_MULTIPLIER = 3f

object ListItemPicker {

    @Suppress("MagicNumber")
    enum class DisplayMode(
        val visibleItemCount: Int
    ) {

        Compact(3),
        Normal(5),
    }
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun <T> ListItemPicker(
    items: PersistentList<T>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    displayMode: ListItemPicker.DisplayMode = ListItemPicker.DisplayMode.Compact,
    dividersColor: Color = RadixTheme.colors.divider,
    textStyle: TextStyle = RadixTheme.typography.body1Regular,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val visibleItemsMiddle = displayMode.visibleItemCount / 2
    val listStartIndex = remember(items, selectedValue) { items.indexOf(selectedValue) }

    val itemHeightPixels = remember { mutableStateOf(0) }
    val itemHeightDp = pixelsToDp(itemHeightPixels.value)
    val fadingEdgeGradient = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.5f to Color.Black,
            1f to Color.Transparent
        )
    }

    val verticalContentPadding = itemHeightDp * visibleItemsMiddle
    val lazyListState = rememberSaveable(items, saver = LazyListState.Saver) {
        LazyListState(
            firstVisibleItemIndex = listStartIndex,
            firstVisibleItemScrollOffset = 0
        )
    }
    val flingBehavior = rememberSnapperFlingBehavior(
        lazyListState = lazyListState,
        springAnimationSpec = spring(),
        decayAnimationSpec = exponentialDecay(frictionMultiplier = SCROLL_FRICTION_MULTIPLIER),
        endContentPadding = verticalContentPadding
    )

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .map { index -> items.getOrNull(index) }
            .distinctUntilChanged()
            .collect { item -> item?.let { onValueChange(it) } }
    }

    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * displayMode.visibleItemCount)
                .fadingEdge(fadingEdgeGradient),
            state = lazyListState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(
                vertical = verticalContentPadding
            ) + contentPadding
        ) {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 34.dp)
                        .onSizeChanged { size -> itemHeightPixels.value = size.height },
                    contentAlignment = contentAlignment
                ) {
                    Text(
                        text = label(item),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = textStyle,
                        color = RadixTheme.colors.text
                    )
                }
            }
        }

        HorizontalDivider(
            color = dividersColor,
            modifier = Modifier.offset(y = itemHeightDp * visibleItemsMiddle)
        )

        HorizontalDivider(
            color = dividersColor,
            modifier = Modifier.offset(y = itemHeightDp * (visibleItemsMiddle + 1))
        )
    }
}

@Composable
private fun pixelsToDp(pixels: Int) = with(LocalDensity.current) { pixels.toDp() }

private fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(
        compositingStrategy = CompositingStrategy.Offscreen
    )
    .drawWithContent {
        drawContent()
        drawRect(
            brush = brush,
            blendMode = BlendMode.DstIn
        )
    }

@Composable
@Preview(showBackground = true)
private fun ListItemPickerPreview() {
    RadixWalletPreviewTheme {
        ListItemPicker(
            modifier = Modifier.fillMaxWidth(),
            selectedValue = "All (Recommended)",
            onValueChange = {},
            items = persistentListOf("All (Recommended)", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
            label = { it }
        )
    }
}
