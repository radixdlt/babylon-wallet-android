package com.babylon.wallet.android.presentation.accessfactorsources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.utils.biometricAuthenticate

@Composable
fun AccessFactorSourcesDialog(
    modifier: Modifier = Modifier,
    viewModel: AccessFactorSourcesViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        context.biometricAuthenticate { isAuthenticated ->
            viewModel.biometricAuthenticationCompleted(isAuthenticated)
            if (isAuthenticated.not()) {
                onDismiss()
            }
        }
    }

    AccessFactorSourceBottomSheetContent(
        modifier = modifier,
        onDismiss = onDismiss
    )
}

@Composable
private fun AccessFactorSourceBottomSheetContent(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    BottomSheetDialogWrapper(
        modifier = modifier,
        onDismiss = {
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                .background(RadixTheme.colors.defaultBackground),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                style = RadixTheme.typography.title,
                text = stringResource(id = R.string.derivePublicKeys_titleCreateAccount)
            )
        }
    }
}

@Preview
@Composable
fun AccessFactorSourcesDialogPreview() {
    AccessFactorSourceBottomSheetContent(
        onDismiss = {}
    )
}
