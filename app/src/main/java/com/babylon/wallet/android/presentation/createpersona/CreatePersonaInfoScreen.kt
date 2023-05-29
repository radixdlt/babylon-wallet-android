package com.babylon.wallet.android.presentation.createpersona

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.InfoLink
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun CreatePersonaInfoScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueClick: () -> Unit,
) {
    CreatePersonaInfoContent(
        onBackClick = onBackClick,
        modifier = modifier,
        onContinueClick = onContinueClick
    )
}

@Composable
private fun CreatePersonaInfoContent(
    onBackClick: () -> Unit,
    modifier: Modifier,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painterResource(id = R.drawable.ic_arrow_back),
                tint = RadixTheme.colors.gray1,
                contentDescription = "navigate back"
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = "",
                placeholder = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                fallback = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                error = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RadixTheme.shapes.roundedRectSmall)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.createPersona_introduction_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(22.dp))
            InfoLink(
                stringResource(com.babylon.wallet.android.R.string.createPersona_introduction_learnAboutPersonas),
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.createPersona_introduction_subtitle1),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(id = com.babylon.wallet.android.R.string.createPersona_introduction_subtitle2),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))
            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                onClick = onContinueClick,
                text = stringResource(id = com.babylon.wallet.android.R.string.continue_button_title)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreatePersonaInfoContentPreview() {
    RadixWalletTheme {
        CreatePersonaInfoContent(
            onBackClick = {},
            modifier = Modifier,
            onContinueClick = {}
        )
    }
}
