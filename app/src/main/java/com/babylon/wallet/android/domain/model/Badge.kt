package com.babylon.wallet.android.domain.model

import android.net.Uri
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem

data class Badge(
    val address: String,
    val nameMetadataItem: NameMetadataItem? = null,
    val iconMetadataItem: IconUrlMetadataItem? = null
) {

    val name: String?
        get() = nameMetadataItem?.name

    val icon: Uri?
        get() = iconMetadataItem?.url

}
