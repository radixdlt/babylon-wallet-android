package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val VISIBLE_ITEM_COUNT = 3

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun <T> ListItemPicker(
    items: PersistentList<T>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    dividersColor: Color = RadixTheme.colors.gray3,
    textStyle: TextStyle = RadixTheme.typography.body1Regular
) {
    val visibleItemsMiddle = VISIBLE_ITEM_COUNT / 2
    val listScrollCount = Integer.MAX_VALUE
    val listScrollMiddle = listScrollCount / 2
    val listStartIndex = listScrollMiddle - listScrollMiddle % items.size - visibleItemsMiddle + items.indexOf(selectedValue)

    fun getItem(index: Int) = items[index % items.size]

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = listStartIndex)
    val flingBehavior = rememberSnapperFlingBehavior(
        lazyListState = lazyListState,
        springAnimationSpec = spring(),
        decayAnimationSpec = exponentialDecay(frictionMultiplier = 5f)
    )

    val itemHeightPixels = remember { mutableStateOf(0) }
    val itemHeightDp = pixelsToDp(itemHeightPixels.value)
    val fadingEdgeGradient = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.5f to Color.Black,
            1f to Color.Transparent
        )
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .map { index -> getItem(index + visibleItemsMiddle) }
            .distinctUntilChanged()
            .collect { item -> onValueChange(item) }
    }

    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeightDp * VISIBLE_ITEM_COUNT)
                .fadingEdge(fadingEdgeGradient),
            state = lazyListState,
            flingBehavior = flingBehavior,
        ) {
            items(listScrollCount) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 34.dp)
                        .onSizeChanged { size -> itemHeightPixels.value = size.height },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(getItem(index)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = textStyle
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
