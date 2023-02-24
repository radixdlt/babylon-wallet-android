package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun PersonaRoundedAvatar(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        placeholder = painterResource(id = R.drawable.img_placeholder),
        fallback = painterResource(id = R.drawable.img_placeholder),
        error = painterResource(id = R.drawable.img_placeholder),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RadixTheme.shapes.circle)
    )
}
