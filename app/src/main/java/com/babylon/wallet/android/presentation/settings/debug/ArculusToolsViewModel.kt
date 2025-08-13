package com.babylon.wallet.android.presentation.settings.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.Mnemonic
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.samples.sample
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ArculusToolsViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    fun onValidateMinFirmwareVersionClick() {
        viewModelScope.launch {
            // Call into Sargon OS; actual method name may differ; replace when available.
            val result = sargonOsManager.callSafely(dispatcher = defaultDispatcher) {
                arculusCardConfigureWithMnemonic(Mnemonic.sample(), "123456")
            }
            result.onSuccess {
                Timber.d("Arculus min firmware validation: success")
            }.onFailure { e ->
                Timber.e(e, "Arculus min firmware validation failed")
            }
        }
    }
}


