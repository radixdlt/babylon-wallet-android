package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme.dimensions
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun CreatePersonaInfoScreen(
    modifier: Modifier = Modifier,
    onInfoClick: (GlossaryItem) -> Unit,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit
) {
    CreatePersonaInfoContent(
        modifier = modifier,
        onInfoClick = onInfoClick,
        onContinueClick = onContinueClick,
        onBackClick = onBackClick,
    )
}

@Composable
private fun CreatePersonaInfoContent(
    modifier: Modifier,
    onInfoClick: (GlossaryItem) -> Unit,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = com.babylon.wallet.android.R.string.empty),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                text = stringResource(id = com.babylon.wallet.android.R.string.createPersona_introduction_continue)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = dimensions.paddingLarge,
                    vertical = dimensions.paddingDefault
                )
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = "",
                placeholder = painterResource(id = DSR.ic_persona),
                fallback = painterResource(id = DSR.ic_persona),
                error = painterResource(id = DSR.ic_persona),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RadixTheme.shapes.circle)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = stringResource(id = R.string.createPersona_introduction_title),
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            InfoButton(
                modifier = Modifier.padding(
                    horizontal = dimensions.paddingDefault,
                    vertical = dimensions.paddingDefault
                ),
                text = stringResource(id = R.string.createPersona_introduction_learnAboutPersonas),
                onClick = {
                    onInfoClick(GlossaryItem.personas)
                }
            )
            Spacer(modifier = Modifier.height(dimensions.paddingDefault))
            Text(
                text = stringResource(id = R.string.createPersona_introduction_subtitle1),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(id = R.string.createPersona_introduction_subtitle2),
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.Center
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
            onContinueClick = {},
            onInfoClick = {}
        )
    }
}
