package com.babylon.wallet.android.presentation.dialogs.info

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    val glossaryItem: StateFlow<GlossaryItem?> = savedStateHandle.getStateFlow(ARG_GLOSSARY_ITEM, null)

    fun onGlossaryItemClick(glossaryItem: String) {
        viewModelScope.launch {
            // The markdown library seems to have a bug that doesn't update its content properly
            // When you click on glossary items markdown won't (sometimes) update its content.
            // If you want to test it just comment out the next two lines.
            savedStateHandle[ARG_GLOSSARY_ITEM] = null
            delay(SHORT_DELAY)

            savedStateHandle[ARG_GLOSSARY_ITEM] = GlossaryItem.valueOf(glossaryItem)
        }
    }

    companion object {
        const val GLOSSARY_ANCHOR = "?glossaryAnchor="
        private const val SHORT_DELAY = 25L
    }
}
