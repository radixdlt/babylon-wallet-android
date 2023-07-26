package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.DAppWithMetadata

@Composable
fun DAppWithMetadata?.displayName(): String {
    return this?.name?.let {
        it.ifEmpty {
            stringResource(id = R.string.dAppRequest_metadata_unknownName)
        }
    } ?: stringResource(id = R.string.dAppRequest_metadata_unknownName)
}
