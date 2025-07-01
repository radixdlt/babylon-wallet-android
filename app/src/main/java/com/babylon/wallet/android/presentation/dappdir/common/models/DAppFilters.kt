package com.babylon.wallet.android.presentation.dappdir.common.models

data class DAppFilters(
    val searchTerm: String = "",
    val selectedTags: Set<String> = emptySet(),
    val availableTags: Set<String> = emptySet()
) {

    fun isTagSelected(tag: String) = tag in selectedTags
}
