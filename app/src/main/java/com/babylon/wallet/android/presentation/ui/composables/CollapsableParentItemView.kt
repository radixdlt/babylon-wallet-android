package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.theme.RadixGrey2

@Composable
fun CollapsableParentItemView(
    nftImageUrl: String?,
    nftName: String?,
    nftsInCirculation: String?,
    nftsInPossession: String?,
    nftChildCount: Int,
    collapsed: Boolean = false,
    arrowText: String,
    parentSectionClick: () -> Unit,
) {
    val bottomPadding = if (collapsed) 8.dp else 0.dp
    Box(
        modifier = Modifier
            .animateContentSize()
            .padding(20.dp, 10.dp, 20.dp, bottomPadding)
            .clickable { parentSectionClick() }
    ) {
        if (collapsed) {
            if (nftChildCount > 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(113.dp)
                        .padding(20.dp, 10.dp, 20.dp, 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = Color.White,
                    elevation = 4.dp
                ) {}
            }
            if (nftChildCount >= 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(103.dp)
                        .padding(10.dp, 10.dp, 10.dp, 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = Color.White,
                    elevation = 4.dp
                ) {}
            }
        }
        val bottomCorners = if (collapsed) 8.dp else 0.dp
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(93.dp),
            shape = RoundedCornerShape(8.dp, 8.dp, bottomCorners, bottomCorners),
            backgroundColor = Color.White,
            elevation = 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 25.dp, vertical = 0.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = nftImageUrl,
                        placeholder = painterResource(id = R.drawable.img_placeholder),
                        fallback = painterResource(id = R.drawable.img_placeholder)
                    ),
                    contentDescription = "Nft icon",
                    contentScale = ContentScale.FillWidth
                )
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    nftName?.let { name ->
                        if (name.isNotEmpty()) {
                            Text(
                                name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2
                            )
                        }
                    }
                    Text(
                        "$nftsInPossession of $nftsInCirculation",
                        color = RadixGrey2,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = arrowText,
                    color = RadixGrey2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CollapsableParentItemPreview() {
    CollapsableParentItemView(
        "https://images.unsplash.com/photo-1628373383885-4be0bc0172fa?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=130&q=80",
        "Rypto Punks",
        "300,000",
        "1",
        3,
        false,
        stringResource(id = R.string.show_plus)
    ) { }
}