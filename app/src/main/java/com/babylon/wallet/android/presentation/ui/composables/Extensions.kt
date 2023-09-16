package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.behaviours.ResourceBehaviour

@Composable
fun Resource.Tag.name(): String {
    return when (this) {
        is Resource.Tag.Official -> "RADIX NETWORK"
        is Resource.Tag.Dynamic -> name
    }
}

@Composable
fun ResourceBehaviour.name(isXrd: Boolean = false): String {
    return when (this) {
        ResourceBehaviour.PERFORM_MINT_BURN -> stringResource(
            id = if (isXrd) R.string.accountSettings_behaviors_supplyFlexibleXrd else title
        )

        else -> stringResource(id = title)
    }
}

@Composable
fun ResourceBehaviour.icon(): Painter = painterResource(id = icon)
