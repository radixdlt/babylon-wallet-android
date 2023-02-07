package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun BottomContinueButton(onLoginClick: () -> Unit, loginButtonEnabled: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Divider(color = RadixTheme.colors.gray5)
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            onClick = onLoginClick,
            enabled = loginButtonEnabled,
            text = stringResource(id = R.string.continue_button_title)
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
    }
}
