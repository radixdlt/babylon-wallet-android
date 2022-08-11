package com.babylon.wallet.android.domain

// WARNING: this is not the actual data structure of the domain model and
// might change completely in the future or not even used and thus drop it ,
// since we don't have any knowledge of the data layer.
//
enum class AssetType(val typeId: String) {
    TOKEN("token"),
    NTF("nft"),
    POOL_SHARE("pool_share"),
    BADGE("badge");

    companion object {
        fun fromTypeId(typeId: String): AssetType = requireNotNull(
            values().find { type ->
                type.typeId == typeId
            }
        )
    }
}
