package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServer
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelaysRepositoryImpl
import com.babylon.wallet.android.fakes.FakeIrohRelayConfig
import com.babylon.wallet.android.fakes.FakeRadixConnectPreferences
import com.babylon.wallet.android.presentation.StateViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.domain.IrohRelay
import rdx.works.core.domain.SavedIrohRelays

@OptIn(ExperimentalCoroutinesApi::class)
class IrohRelayServerDetailsViewModelTest : StateViewModelTest<IrohRelayServerDetailsViewModel>() {

    private val preferences = FakeRadixConnectPreferences()
    private val relayConfig = FakeIrohRelayConfig()
    private var relayServer: IrohRelayServer? = null

    override fun initVM(): IrohRelayServerDetailsViewModel = IrohRelayServerDetailsViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                ARG_IROH_RELAY_URL to (relayServer as? IrohRelayServer.Custom)?.url,
                ARG_IROH_RELAY_IS_PUBLIC to (relayServer == IrohRelayServer.Public)
            )
        ),
        irohRelaysRepository = IrohRelaysRepositoryImpl(
            radixConnectPreferences = preferences,
            irohRelayConfig = relayConfig,
            defaultDispatcher = coroutineRule.dispatcher
        ),
        irohRelayConfig = relayConfig,
        defaultDispatcher = coroutineRule.dispatcher
    )

    @Test
    fun `a new relay server is validated and added`() = runTest {
        val vm = vm.value
        advanceUntilIdle()

        vm.onRelayUrlChanged("wss://relay.example.com")
        advanceUntilIdle()
        assertEquals(
            IrohRelayServerDetailsViewModel.FieldError.Invalid("the scheme must be https or http"),
            vm.state.value.fieldError
        )
        assertFalse(vm.state.value.isSaveEnabled)

        vm.onRelayUrlChanged(" https://relay.example.com ")
        advanceUntilIdle()
        assertNull(vm.state.value.fieldError)
        assertTrue(vm.state.value.isSaveEnabled)

        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(SavedIrohRelays(others = listOf(IrohRelay("https://relay.example.com"))), preferences.irohRelaysState.value)
    }

    @Test
    fun `while typing the error stays until the new URL is validated`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onRelayUrlChanged("h")
        advanceUntilIdle()
        val invalid = vm.state.value.fieldError
        assertTrue(invalid is IrohRelayServerDetailsViewModel.FieldError.Invalid)

        // Each key keeps what was shown, rather than clearing it for a moment.
        vm.onRelayUrlChanged("ht")
        assertEquals(invalid, vm.state.value.fieldError)
        vm.onRelayUrlChanged("https://relay.example.com")
        assertEquals(invalid, vm.state.value.fieldError)

        advanceUntilIdle()
        assertNull(vm.state.value.fieldError)
        assertTrue(vm.state.value.isSaveEnabled)
    }

    @Test
    fun `a URL typed after a valid one is not saved before it is validated`() = runTest {
        val vm = vm.value
        advanceUntilIdle()
        vm.onRelayUrlChanged("https://relay.example.com")
        advanceUntilIdle()
        vm.onRelayUrlChanged("ftp://relay.example.com")

        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(SavedIrohRelays(), preferences.irohRelaysState.value)
        assertFalse(vm.state.value.isSaveEnabled)
    }

    @Test
    fun `a relay server already listed cannot be added again`() = runTest {
        preferences.irohRelaysState.value = SavedIrohRelays(others = listOf(IrohRelay("https://relay.example.com")))
        val vm = vm.value
        advanceUntilIdle()

        // Pointed out as it is typed, not only once saved.
        vm.onRelayUrlChanged("https://relay.example.com")
        advanceUntilIdle()
        assertEquals(IrohRelayServerDetailsViewModel.FieldError.AlreadyAdded, vm.state.value.fieldError)
        assertFalse(vm.state.value.isSaveEnabled)

        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(1, preferences.irohRelaysState.value.others.size)
    }

    @Test
    fun `an edited relay server can be saved only once changed`() = runTest {
        relayServer = IrohRelayServer.Custom("https://relay.example.com")
        preferences.irohRelaysState.value = SavedIrohRelays(others = listOf(IrohRelay("https://relay.example.com")))
        val vm = vm.value
        advanceUntilIdle()
        assertEquals("https://relay.example.com", vm.state.value.relayUrl)
        assertFalse(vm.state.value.isSaveEnabled)

        vm.onRelayUrlChanged("https://other.example.com")
        advanceUntilIdle()
        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(SavedIrohRelays(others = listOf(IrohRelay("https://other.example.com"))), preferences.irohRelaysState.value)
    }

    @Test
    fun `a relay server can be made current and removed`() = runTest {
        relayServer = IrohRelayServer.Custom("https://relay.example.com")
        preferences.irohRelaysState.value = SavedIrohRelays(others = listOf(IrohRelay("https://relay.example.com")))
        val vm = vm.value
        advanceUntilIdle()
        assertTrue(vm.state.value.canBeMadeCurrent)
        assertTrue(vm.state.value.canBeRemoved)

        vm.onMakeCurrentClick()
        advanceUntilIdle()
        assertEquals(SavedIrohRelays(current = IrohRelay("https://relay.example.com")), preferences.irohRelaysState.value)
        assertFalse(vm.state.value.canBeMadeCurrent)

        vm.onDeleteClick()
        assertTrue(vm.state.value.showDeleteConfirmation)
        vm.onDeleteConfirmationDismissed(confirmed = true)
        advanceUntilIdle()
        assertEquals(SavedIrohRelays(), preferences.irohRelaysState.value)
    }

    @Test
    fun `the public relays show their hosts and cannot be edited or removed`() = runTest {
        relayServer = IrohRelayServer.Public
        preferences.irohRelaysState.value = SavedIrohRelays(current = IrohRelay("https://relay.example.com"))
        val vm = vm.value
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.isPublic)
        assertEquals(FakeIrohRelayConfig.PUBLIC_RELAYS, state.publicRelayUrls)
        assertFalse(state.isEditable)
        assertFalse(state.canBeRemoved)
        assertTrue(state.canBeMadeCurrent)

        vm.onMakeCurrentClick()
        advanceUntilIdle()
        assertEquals(SavedIrohRelays(others = listOf(IrohRelay("https://relay.example.com"))), preferences.irohRelaysState.value)
    }

    @Test
    fun `nothing can be added when the iroh library is not available`() = runTest {
        relayConfig.isLibraryAvailable = false
        val vm = vm.value
        advanceUntilIdle()

        vm.onRelayUrlChanged("https://relay.example.com")
        advanceUntilIdle()

        assertFalse(vm.state.value.isLibraryAvailable)
        assertFalse(vm.state.value.isEditable)
        assertFalse(vm.state.value.isSaveEnabled)
    }
}
