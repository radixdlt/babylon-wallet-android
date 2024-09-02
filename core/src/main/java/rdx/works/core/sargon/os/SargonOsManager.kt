package rdx.works.core.sargon.os

import com.radixdlt.sargon.Bios
import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.os.driver.AndroidProfileStateChangeDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.di.ApplicationScope
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SargonOsManager @Inject constructor(
    bios: Bios,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private val _sargonState = MutableStateFlow<SargonOsState>(SargonOsState.Idle)

    private val sargonOs: Flow<SargonOs> = _sargonState.filterIsInstance<SargonOsState.Booted>().map { it.os }

    init {
        applicationScope.launch {
            withContext(defaultDispatcher) {
                val os = SargonOs.boot(bios)
                _sargonState.update { SargonOsState.Booted(os) }
            }
        }
    }
}

sealed interface SargonOsState {
    data object Idle: SargonOsState
    data class Booted(
        val os: SargonOs
    ): SargonOsState
}