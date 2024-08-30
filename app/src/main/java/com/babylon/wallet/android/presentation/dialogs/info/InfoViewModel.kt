package com.babylon.wallet.android.presentation.dialogs.info

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    val glossaryItem: StateFlow<GlossaryItem?> = savedStateHandle.getStateFlow(ARG_GLOSSARY_ITEM, null)

    fun onGlossaryItemClick(glossaryItem: String) {
        savedStateHandle[ARG_GLOSSARY_ITEM] = GlossaryItem.valueOf(glossaryItem)
    }

    companion object {
        const val GLOSSARY_ANCHOR = "?glossaryAnchor="
    }
}
