package com.babylon.wallet.android.domain.model.resources

import android.net.Uri
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem

data class Badge(
    val address: String,
    private val nameMetadataItem: NameMetadataItem? = null,
    private val iconMetadataItem: IconUrlMetadataItem? = null
) {

    val name: String?
        get() = nameMetadataItem?.name

    val icon: Uri?
        get() = iconMetadataItem?.url
}
