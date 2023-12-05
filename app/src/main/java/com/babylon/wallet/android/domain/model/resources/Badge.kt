package com.babylon.wallet.android.domain.model.resources

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.iconUrl
import com.babylon.wallet.android.domain.model.resources.metadata.name

data class Badge(
    val address: String,
    val metadata: List<Metadata> = emptyList()
) {

    val name: String?
        get() = metadata.name()

    val icon: Uri?
        get() = metadata.iconUrl()
}
