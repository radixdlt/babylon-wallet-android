package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.utils.truncatedHash
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rdx.works.core.qr.QRCodeGenerator
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork

@Composable
fun AccountQRCodeView(
    modifier: Modifier = Modifier,
    accountAddress: String,
) {
    val context = LocalContext.current.applicationContext
    val useCaseProvider = remember(context) {
        EntryPoints.get(context, AccountQRCodeViewEntryPoint::class.java)
    }
    var accountQRCodeViewDataHolder: AccountQRCodeViewDataHolder? by remember { mutableStateOf(null) }
    LaunchedEffect(useCaseProvider, accountAddress) {
        useCaseProvider.profileUseCase().accountOnCurrentNetwork(accountAddress)?.let { account ->
            accountQRCodeViewDataHolder = AccountQRCodeViewDataHolder(
                accountName = account.displayName,
                accountAppearanceId = account.appearanceID
            )
        }
    }

    Column(
        modifier = modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        accountQRCodeViewDataHolder?.let { dataHolder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(AccountGradientList[dataHolder.accountAppearanceId % AccountGradientList.size]),
                        RadixTheme.shapes.roundedRectSmall
                    )
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.padding(end = RadixTheme.dimensions.paddingMedium),
                    text = dataHolder.accountName,
                    style = RadixTheme.typography.body1Header,
                    maxLines = 1,
                    color = RadixTheme.colors.white,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = accountAddress.truncatedHash(),
                    color = RadixTheme.colors.white.copy(alpha = 0.8f),
                    maxLines = 1,
                    style = RadixTheme.typography.body2HighImportance
                )
            }
        }

        val qrCode = remember(accountAddress) {
            QRCodeGenerator.forAccount(accountAddress)
        }

        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1f),
            model = qrCode,
            contentDescription = null
        )
    }
}

private data class AccountQRCodeViewDataHolder(
    val accountName: String,
    val accountAppearanceId: Int
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AccountQRCodeViewEntryPoint {
    fun profileUseCase(): GetProfileUseCase
}
