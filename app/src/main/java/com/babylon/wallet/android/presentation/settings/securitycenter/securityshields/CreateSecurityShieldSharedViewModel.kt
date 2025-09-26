package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateSecurityShieldSharedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val args = CreateSecurityShieldArgs(savedStateHandle)
}
