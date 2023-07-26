package com.babylon.wallet.android.domain

import android.content.Context
import com.babylon.wallet.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DefaultStringProvider @Inject constructor(
    @ApplicationContext context: Context
) {
    val unknownDapp = context.getString(R.string.dAppRequest_metadata_unknownName)
}
