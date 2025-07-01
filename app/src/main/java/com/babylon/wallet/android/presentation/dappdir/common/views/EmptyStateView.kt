package com.babylon.wallet.android.presentation.dappdir.common.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.InfoButton

@Composable
fun DAppsEmptyStateView(
    title: String,
    onInfoClick: (GlossaryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            text = title,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )

        InfoButton(
            text = stringResource(id = R.string.infoLink_title_dapps),
            onClick = { onInfoClick(GlossaryItem.dapps) }
        )
    }
}
