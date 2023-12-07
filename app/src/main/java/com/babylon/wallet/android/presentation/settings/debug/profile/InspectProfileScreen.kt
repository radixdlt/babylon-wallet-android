package com.babylon.wallet.android.presentation.settings.debug.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import rdx.works.profile.data.model.Profile

@Composable
fun InspectProfileScreen(
    viewModel: InspectProfileViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
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
        }
    ) { padding ->
        if (state.isRawProfileVisible) {
            RawProfileContent(
                modifier = modifier.padding(padding),
                profileSnapshot = state.rawSnapshot.orEmpty()
            )
        } else {
            val profile = state.profile
            if (profile != null) {
                ProfileContent(modifier = modifier.padding(padding), profile = profile)
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

@Composable
private fun ProfileContent(
    modifier: Modifier,
    profile: Profile
) {

}
