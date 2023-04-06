package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme

/**
 * use this as a Root composable of new dialog composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetWrapper(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    scrimColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        shape = RadixTheme.shapes.roundedRectTopDefault,
        scrimColor = scrimColor,
        dragHandle = {
            BottomDialogDragHandle(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectTopDefault)
                    .padding(vertical = RadixTheme.dimensions.paddingDefault)
            )
        }
    ) {
        content()
    }
}

/**
 * use this if you want AlertDialog style usage, like BasicPromptAlertDialog - not using new route
 */
@Composable
fun BottomSheetDialogWrapper(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        BottomSheetWrapper(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            scrimColor = Color.Transparent,
            content = content,
        )
    }
}

@Composable
private fun BottomDialogDragHandle(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(38.dp, 4.dp)
                .background(color = RadixTheme.colors.gray4, shape = RadixTheme.shapes.circle)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun BasicPromptAlertDialog(
    finish: (accepted: Boolean) -> Unit,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(id = R.string.confirm),
    dismissText: String = stringResource(id = R.string.cancel),
    confirmTextColor: Color = RadixTheme.colors.blue2
) {
    AlertDialog(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectSmall)
            .clip(RadixTheme.shapes.roundedRectSmall),
        onDismissRequest = { finish(false) },
        confirmButton = {
            RadixTextButton(text = confirmText, onClick = { finish(true) }, contentColor = confirmTextColor)
        },
        dismissButton = {
            RadixTextButton(text = dismissText, onClick = { finish(false) })
        },
        title = title,
        text = text
    )
}

@Composable
fun NotSecureAlertDialog(
    finish: (accepted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPromptAlertDialog(modifier = modifier, finish = finish, title = {
        Text(
            text = stringResource(id = R.string.please_confirm_dialog_title),
            style = RadixTheme.typography.body2Header,
            color = RadixTheme.colors.gray1
        )
    }, text = {
            Text(
                text = stringResource(id = R.string.please_confirm_dialog_body),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
        })
}

@Composable
fun SomethingWentWrongDialog(modifier: Modifier = Modifier, onDismissRequest: () -> Unit) {
    BottomSheetWrapper(onDismissRequest = onDismissRequest) {
        SomethingWentWrongDialogContent(
            title = stringResource(id = R.string.something_went_wrong),
            subtitle = stringResource(id = R.string.please_confirm_dialog_body),
            modifier = modifier
        )
    }
}

@Composable
private fun SomethingWentWrongDialogContent(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(RadixTheme.colors.defaultBackground)
            .padding(vertical = 40.dp, horizontal = RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.size(90.dp),
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_warning_error
            ),
            contentDescription = null,
            tint = RadixTheme.colors.orange1
        )
        Text(
            text = title,
            style = RadixTheme.typography.title,
            color = RadixTheme.colors.gray1
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        Text(
            text = subtitle,
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray1
        )
    }
}

@Composable
@Preview
fun SomethingWentWrongDialogPreview() {
    RadixWalletTheme {
        SomethingWentWrongDialogContent("Title", "Subtitle")
    }
}
