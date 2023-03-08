package com.babylon.wallet.android.presentation.ui.composables

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalTextStyle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.Address
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionableAddressView(
    modifier: Modifier = Modifier,
    address: String,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    val addressWithType = resolveAddressWithType(address = address)
    val actions = resolveActions(address = addressWithType)
    var isDropdownMenuExpanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = modifier.combinedClickable(
                onClick = { actions.primary.onAction() },
                onLongClick = { isDropdownMenuExpanded = true }
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)
        ) {
            Text(
                text = addressWithType.truncated,
                color = textColor,
                maxLines = 1,
                style = textStyle
            )

            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(id = actions.primary.icon),
                contentDescription = actions.primary.name,
                tint = iconColor,
            )
        }

        DropdownMenu(
            modifier = Modifier.background(RadixTheme.colors.defaultBackground),
            expanded = isDropdownMenuExpanded,
            onDismissRequest = { isDropdownMenuExpanded = false }
        ) {
            actions.all.forEach {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = it.name,
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.defaultText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = it.icon),
                            contentDescription = it.name,
                            tint = RadixTheme.colors.defaultText
                        )
                    },
                    onClick = {
                        isDropdownMenuExpanded = false
                        it.onAction()
                    },
                    contentPadding = PaddingValues(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    )
                )
            }
        }
    }
}

@Composable
private fun resolveAddressWithType(
    address: String
): Address = remember(address) { Address.from(address) }

@Suppress("SwallowedException")
@Composable
private fun resolveActions(
    address: Address
): ActionableAddressActions {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val copyAction = ActionableAddressAction(
        name = stringResource(
            id = R.string.action_copy,
            address.type.localisedName()
        ),
        icon = R.drawable.ic_copy
    ) {
        clipboardManager.setText(AnnotatedString(address.address))

        // From Android 13, the system handles the copy confirmation
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }

    val openExternalAction = ActionableAddressAction(
        name = stringResource(
            id = R.string.action_open_in_dashboard,
            address.type.localisedName()
        ),
        icon = R.drawable.ic_external_link
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = address.toDashboardUrl().toUri()
        }

        try {
            context.startActivity(intent)
        } catch (activityNotFound: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_web_browser_installed, Toast.LENGTH_SHORT).show()
        }
    }

    return remember(address) {
        if (address.type == Address.Type.ACCOUNT) {
            ActionableAddressActions(
                primary = copyAction,
                secondary = openExternalAction
            )
        } else {
            ActionableAddressActions(
                primary = openExternalAction,
                secondary = copyAction
            )
        }
    }
}

@Composable
private fun Address.Type.localisedName(): String = when (this) {
    Address.Type.PACKAGE -> stringResource(id = R.string.address_package)
    Address.Type.RESOURCE -> stringResource(id = R.string.address_resource)
    Address.Type.ACCOUNT -> stringResource(id = R.string.address_account)
    Address.Type.TRANSACTION -> stringResource(id = R.string.address_transaction)
    Address.Type.COMPONENT -> stringResource(id = R.string.address_component)
}.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}

private data class ActionableAddressActions(
    val primary: ActionableAddressAction,
    val secondary: ActionableAddressAction
) {

    val all = listOf(primary, secondary)
}

private data class ActionableAddressAction(
    val name: String,
    @DrawableRes val icon: Int,
    val onAction: () -> Unit
)
