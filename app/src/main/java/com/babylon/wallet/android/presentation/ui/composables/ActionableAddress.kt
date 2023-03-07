package com.babylon.wallet.android.presentation.ui.composables

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.AddressHRP
import com.babylon.wallet.android.utils.truncatedHash
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionableAddress(
    modifier: Modifier = Modifier,
    address: String,
    textStyle: TextStyle,
    contentColor: Color
) {
    val addressWithType = resolveAddressWithType(address = address)
    val actions = resolveActions(addressWithType = addressWithType)

    Row(
        modifier = modifier.combinedClickable(
            onClick = {
                actions.primary.onAction()
            },
            onLongClick = {

            }
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)
    ) {

        Text(
            modifier = Modifier.weight(1f, false),
            text = addressWithType.truncated,
            color = contentColor,
            maxLines = 1,
            style = textStyle
        )

        Icon(
            modifier = Modifier.size(14.dp),
            painter = painterResource(id = actions.primary.icon),
            contentDescription = null,
            tint = contentColor,
        )
    }
}

@Composable
private fun resolveAddressWithType(
    address: String
): AddressWithType = remember(address) {
    AddressWithType(
        address = address,
        truncated = address.truncatedHash(),
        type = AddressType.from(address)
    )
}

@Composable
private fun resolveActions(
    addressWithType: AddressWithType
): ActionableAddressActions {
    val clipboardManager = LocalClipboardManager.current
    val copyAction = ActionableAddressAction(
        name = stringResource(
            id = R.string.action_copy,
            addressWithType.type.previewName()
        ),
        icon = R.drawable.ic_copy
    ) {
        clipboardManager.setText(AnnotatedString(addressWithType.address))
    }

    val context = LocalContext.current
    val openExternalAction = ActionableAddressAction(
        name = stringResource(
            id = R.string.action_open_in_dashboard,
            addressWithType.type.previewName()
        ),
        icon = R.drawable.ic_external_link
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = addressWithType.toDashboardUri()
        }

        try {
            context.startActivity(intent)
        } catch (activityNotFound: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_web_browser_installed, Toast.LENGTH_SHORT).show()
        }
    }

    return remember(addressWithType) {
        if (addressWithType.type == AddressType.ACCOUNT) {
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

private data class AddressWithType(
    val address: String,
    val truncated: String,
    val type: AddressType
) {

    private val isNft: Boolean
        get() = type == AddressType.RESOURCE && address.split(":").size > 1

    fun toDashboardUri(): Uri {
        val suffix = when {
            isNft -> "nft/$address"
            else -> "${type.name}/$address"
        }

        return Uri.parse("$DASHBOARD_BASE_URL/$suffix")
    }

    companion object {
        private const val DASHBOARD_BASE_URL = "https://betanet-dashboard.radixdlt.com/"
    }

}

private enum class AddressType(
    val prefix: String
) {
    PACKAGE(AddressHRP.PACKAGE),
    RESOURCE(AddressHRP.RESOURCE),
    ACCOUNT(AddressHRP.ACCOUNT),
    TRANSACTION(AddressHRP.TRANSACTION),
    COMPONENT(AddressHRP.COMPONENT);

    fun previewName() = name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    companion object {

        fun from(address: String): AddressType = AddressType.values().find {
            address.startsWith(it.prefix)
        } ?: TRANSACTION

    }
}

private data class ActionableAddressActions(
    val primary: ActionableAddressAction,
    val secondary: ActionableAddressAction
)

private data class ActionableAddressAction(
    val name: String,
    @DrawableRes val icon: Int,
    val onAction: () -> Unit
)
