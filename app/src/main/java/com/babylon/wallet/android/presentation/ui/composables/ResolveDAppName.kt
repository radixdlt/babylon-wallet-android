package com.babylon.wallet.android.presentation.ui.composables

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import rdx.works.core.domain.DApp

@Composable
fun DApp?.displayName(): String {
    return this?.name.dAppDisplayName()
}

@Composable
fun String?.dAppDisplayName(): String {
    return this.orEmpty().ifEmpty {
        stringResource(id = R.string.dAppRequest_metadata_unknownName)
    }
}

fun String?.dAppDisplayName(context: Context): String {
    return this.orEmpty().ifEmpty {
        context.resources.getString(R.string.dAppRequest_metadata_unknownName)
    }
}
