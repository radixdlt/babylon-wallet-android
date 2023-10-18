package com.babylon.wallet.android.domain.model

sealed interface Tag {
    data object Official : Tag

    data class Dynamic(
        val name: String
    ) : Tag
}
