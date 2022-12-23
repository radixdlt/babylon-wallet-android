package com.babylon.wallet.android.presentation.accountpreference

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar

@Composable
fun AccountPreferenceScreen(
    viewModel: AccountPreferenceViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val state = viewModel.state
    AccountPreferenceContent(
        onBackClick = onBackClick,
        modifier = modifier
            .systemBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        onGetFreeXrdClick = viewModel::onGetFreeXrdClick,
        canUseFaucet = state.canUseFaucet,
        loading = state.isLoading
    )
}

@Composable
private fun AccountPreferenceContent(
    onBackClick: () -> Unit,
    onGetFreeXrdClick: () -> Unit,
    canUseFaucet: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.account_preference),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(RadixTheme.colors.gray5)
                    .padding(RadixTheme.dimensions.paddingLarge)

            ) {
                RadixSecondaryButton(modifier = Modifier.fillMaxWidth(), text = stringResource(R.string.get_free_xrd),
                    onClick = onGetFreeXrdClick,
                    enabled = !loading && canUseFaucet)
            }
            if (loading) {
                FullscreenCircularProgressContent()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountPreferencePreview() {
    BabylonWalletTheme {
        AccountPreferenceContent(
            onBackClick = {},
            onGetFreeXrdClick = {},
            canUseFaucet = true,
            loading = false
        )
    }
}
