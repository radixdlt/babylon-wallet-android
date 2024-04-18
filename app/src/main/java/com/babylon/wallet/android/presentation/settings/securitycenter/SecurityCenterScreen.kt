package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar

@Composable
fun SecurityCenterScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onSecurityFactorsClick: () -> Unit,
) {
    SecurityCenterContent(modifier, onBackClick, onSecurityFactorsClick)
}

@Composable
private fun SecurityCenterContent(modifier: Modifier = Modifier, onBackClick: () -> Unit, onSecurityFactorsClick: () -> Unit) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars,
                contentColor = RadixTheme.colors.gray1,
                containerColor = RadixTheme.colors.gray5
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = "Security Center", // TODO crowdin
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = "Decentralized security settings that give you total control over your wallet’s protection.", // TODO crowdin
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))
            StatusCard()
            SecurityFactorsCard(onSecurityFactorsClick = onSecurityFactorsClick)
            BackupConfigurationCard()
        }
    }
}

@Composable
private fun StatusCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RadixTheme.colors.green1, RadixTheme.shapes.roundedRectMedium)
            .padding(horizontal = RadixTheme.dimensions.paddingLarge, vertical = RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(painter = painterResource(id = DSR.ic_security_center), contentDescription = null, tint = RadixTheme.colors.white)
        Text(
            text = "Your wallet is recoverable", // TODO crowdin
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.white
        )
    }
}

@Composable
private fun SecurityFactorsCard(modifier: Modifier = Modifier, onSecurityFactorsClick: () -> Unit) {
    Row(
        modifier = modifier
            .shadow(6.dp, shape = RadixTheme.shapes.roundedRectMedium)
            .clip(RadixTheme.shapes.roundedRectMedium)
            .clickable {
                onSecurityFactorsClick()
            }
            .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            modifier = Modifier.size(80.dp),
            painter = painterResource(id = DSR.ic_security_center),
            contentDescription = null,
            tint = RadixTheme.colors.green1
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RadixTheme.dimensions.paddingSmall, vertical = RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingSmall, alignment = Alignment.CenterVertically)
        ) {
            Text(
                text = "Security Factors", // TODO crowdin
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = "The keys you use to control your Accounts and Personas", // TODO crowdin
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                Arrangement.spacedBy(space = RadixTheme.dimensions.paddingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(id = DSR.ic_check_circle), contentDescription = null, tint = RadixTheme.colors.green1)
                Text(
                    text = "Active", // TODO crowdin
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.green1
                )
            }
        }
    }
}

@Composable
private fun BackupConfigurationCard() {
    Row(
        modifier = Modifier
            .shadow(6.dp, shape = RadixTheme.shapes.roundedRectMedium)
            .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault, vertical = RadixTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingMedium)
    ) {
        Icon(
            modifier = Modifier.size(80.dp),
            painter = painterResource(id = DSR.ic_security_center),
            contentDescription = null,
            tint = RadixTheme.colors.green1
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = RadixTheme.dimensions.paddingSmall, vertical = RadixTheme.dimensions.paddingDefault),
            verticalArrangement = Arrangement.spacedBy(space = RadixTheme.dimensions.paddingSmall, alignment = Alignment.CenterVertically)
        ) {
            Text(
                text = "Configuration Backup", // TODO crowdin
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.gray1
            )
            Text(
                text = "A backup of your security settings and wallet settings", // TODO crowdin
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                Arrangement.spacedBy(space = RadixTheme.dimensions.paddingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(id = DSR.ic_check_circle), contentDescription = null, tint = RadixTheme.colors.green1)
                Text(
                    text = "Backed up", // TODO crowdin
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.green1
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SecurityCenterContentPreview() {
    RadixWalletTheme {
        SecurityCenterContent(
            onBackClick = {},
            onSecurityFactorsClick = {}
        )
    }
}
