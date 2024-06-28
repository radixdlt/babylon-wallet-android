package com.babylon.wallet.android.presentation.settings.debug.profile

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar

@Composable
fun InspectProfileScreen(
    viewModel: InspectProfileViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.settings_debugSettings_inspectProfile),
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        modifier = Modifier
                            .padding(end = RadixTheme.dimensions.paddingDefault)
                            .background(
                                color = RadixTheme.colors.gray4,
                                shape = RadixTheme.shapes.roundedRectSmall
                            ),
                        onClick = {
                            viewModel.toggleRawProfileVisible(!state.isRawProfileVisible)
                        }
                    ) {
                        Icon(
                            painterResource(
                                id = com.babylon.wallet.android.designsystem.R.drawable.ic_manifest_expand
                            ),
                            tint = Color.Unspecified,
                            contentDescription = null
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            if (state.rawSnapshot != null) {
                val context = LocalContext.current
                FloatingActionButton(
                    shape = CircleShape,
                    containerColor = RadixTheme.colors.gray4,
                    contentColor = RadixTheme.colors.gray1,
                    onClick = {
                        context.getSystemService<android.content.ClipboardManager>()?.let { clipboardManager ->
                            val clipData = ClipData.newPlainText(
                                "Radix Address",
                                state.rawSnapshot
                            )
                            clipboardManager.setPrimaryClip(clipData)
                        }
                    }
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_copy), contentDescription = null)
                }
            }
        }
    ) { padding ->
        if (state.isRawProfileVisible) {
            RawProfileContent(
                modifier = Modifier.padding(padding),
                profileSnapshot = state.rawSnapshot.orEmpty()
            )
        } else {
            val profile = state.profile
            if (profile != null) {
                ProfileContent(modifier = Modifier.padding(padding))
            } else {
                FullscreenCircularProgressContent()
            }
        }
    }
}

@Composable
private fun RawProfileContent(
    modifier: Modifier,
    profileSnapshot: String
) {
    val customTextSelectionColors = TextSelectionColors(
        handleColor = RadixTheme.colors.gray4,
        backgroundColor = RadixTheme.colors.gray4
    )
    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        SelectionContainer {
            Text(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
                text = profileSnapshot,
                color = RadixTheme.colors.gray1,
                fontSize = 13.sp,
                fontFamily = FontFamily(Typeface(android.graphics.Typeface.MONOSPACE)),
            )
        }
    }
}

@Composable
private fun ProfileContent(
    modifier: Modifier
) {
    Text(modifier = modifier.fillMaxSize(), text = "Profile viewer tbd", textAlign = TextAlign.Center)
}
