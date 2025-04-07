package com.babylon.wallet.android.presentation

import app.cash.turbine.test
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessArculusFactorSourceUseCase
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessDeviceFactorSourceUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceDelegate
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessLedgerHardwareWalletFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessOffDeviceMnemonicFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessPasswordFactorSourceUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.GetSignaturesViewModel
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountPath
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.AppPreferences
import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.CompiledTransactionIntent
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Hardened
import com.radixdlt.sargon.Header
import com.radixdlt.sargon.HostInfo
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.OwnedFactorInstance
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileNetwork
import com.radixdlt.sargon.UnsecurifiedHardened
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.decompile
import com.radixdlt.sargon.extensions.default
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.initFromLocal
import com.radixdlt.sargon.extensions.phrase
import com.radixdlt.sargon.extensions.sign
import com.radixdlt.sargon.extensions.unsecuredControllingFactorInstance
import com.radixdlt.sargon.newDeviceFactorSourceBabylon
import com.radixdlt.sargon.newMnemonicSampleDevice
import com.radixdlt.sargon.newMnemonicSampleDeviceOther
import com.radixdlt.sargon.newMnemonicSampleLedger
import com.radixdlt.sargon.os.signing.FactorOutcome
import com.radixdlt.sargon.os.signing.HdSignature
import com.radixdlt.sargon.os.signing.HdSignatureInput
import com.radixdlt.sargon.os.signing.PerFactorOutcome
import com.radixdlt.sargon.os.signing.PerFactorSourceInput
import com.radixdlt.sargon.os.signing.Signable
import com.radixdlt.sargon.os.signing.TransactionSignRequestInput
import com.radixdlt.sargon.samples.sample
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import rdx.works.core.sargon.allAccountsOnCurrentNetwork
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.initBabylon
import rdx.works.profile.domain.GetProfileUseCase


@OptIn(ExperimentalCoroutinesApi::class)
class GetSignaturesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val coroutineRule = TestDispatcherRule(
        dispatcher = testDispatcher
    )
    private val testScope = TestScope(testDispatcher)

    private val deviceMnemonic = MnemonicWithPassphrase.init(phrase = newMnemonicSampleDevice().phrase)
    private val otherDeviceMnemonic = MnemonicWithPassphrase.init(phrase = newMnemonicSampleDeviceOther().phrase)
    private val fakeLedgerMnemonic = MnemonicWithPassphrase.init(phrase = newMnemonicSampleLedger().phrase)
    private val deviceFactorSource1 = newDeviceFactorSourceBabylon(
        isMain = true,
        mnemonicWithPassphrase = deviceMnemonic,
        hostInfo = HostInfo.sample()
    )
    private val deviceFactorSource2 = newDeviceFactorSourceBabylon(
        isMain = false,
        mnemonicWithPassphrase = otherDeviceMnemonic,
        hostInfo = HostInfo.sample()
    )
    private val ledgerFactorSource1 = LedgerHardwareWalletFactorSource.sample()
    private val sampleProfile = Profile(
        header = Header.sample(),
        factorSources = listOf(
            deviceFactorSource1.asGeneral(),
            deviceFactorSource2.asGeneral(),
            ledgerFactorSource1.asGeneral()
        ),
        appPreferences = AppPreferences.default(),
        networks = listOf(
            ProfileNetwork(
                id = NetworkId.MAINNET,
                accounts = listOf(
                    Account.initBabylon(
                        networkId = NetworkId.MAINNET,
                        displayName = DisplayName("Acc1"),
                        hdPublicKey = deviceMnemonic.derivePublicKey(
                            DerivationPath.Account(
                                AccountPath(
                                    networkId = NetworkId.MAINNET,
                                    keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
                                    index = Hardened.Unsecurified(v1 = UnsecurifiedHardened.initFromLocal(0u))
                                )
                            )
                        ),
                        factorSourceId = deviceFactorSource1.id.asGeneral(),
                    ),
                    Account.initBabylon(
                        networkId = NetworkId.MAINNET,
                        displayName = DisplayName("Acc2"),
                        hdPublicKey = otherDeviceMnemonic.derivePublicKey(
                            DerivationPath.Account(
                                AccountPath(
                                    networkId = NetworkId.MAINNET,
                                    keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
                                    index = Hardened.Unsecurified(v1 = UnsecurifiedHardened.initFromLocal(1u))
                                )
                            )
                        ),
                        factorSourceId = deviceFactorSource2.id.asGeneral(),
                    ),
                    Account.initBabylon(
                        networkId = NetworkId.MAINNET,
                        displayName = DisplayName("Acc3"),
                        hdPublicKey = fakeLedgerMnemonic.derivePublicKey(
                            DerivationPath.Account(
                                AccountPath(
                                    networkId = NetworkId.MAINNET,
                                    keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
                                    index = Hardened.Unsecurified(v1 = UnsecurifiedHardened.initFromLocal(2u))
                                )
                            )
                        ),
                        factorSourceId = ledgerFactorSource1.id.asGeneral(),
                    )
                ),
                personas = emptyList(),
                authorizedDapps = emptyList(),
                resourcePreferences = emptyList()
            )
        )
    ).changeGatewayToNetworkId(NetworkId.MAINNET)

    private val ownedFactorInstances = sampleProfile.allAccountsOnCurrentNetwork.map {
        OwnedFactorInstance(
            owner = AddressOfAccountOrPersona.Account(it.address),
            factorInstance = requireNotNull(it.unsecuredControllingFactorInstance)
        )
    }
    private val deviceFactorInstances = ownedFactorInstances.filter { it.factorInstance.factorSourceId.kind == FactorSourceKind.DEVICE }
    private val ledgerFactorInstances =
        ownedFactorInstances.filter { it.factorInstance.factorSourceId.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET }


    private val accessFactorSourcesIOHandler = mockk<AccessFactorSourcesIOHandler>()
    private val accessDeviceFactorSource = mockk<AccessDeviceFactorSourceUseCase>()
    private val accessLedgerHardwareWalletFactorSource = mockk<AccessLedgerHardwareWalletFactorSourceUseCase>()
    private val accessOffDeviceMnemonicFactorSource = mockk<AccessOffDeviceMnemonicFactorSourceUseCase>()
    private val accessArculusFactorSourceUseCase = mockk<AccessArculusFactorSourceUseCase>()
    private val accessPasswordFactorSourceUseCase = mockk<AccessPasswordFactorSourceUseCase>()

    private val signaturesPerInput: Map<PerFactorSourceInput<Signable.Payload, Signable.ID>, PerFactorOutcome<Signable.ID>> = mapOf(
        PerFactorSourceInput<Signable.Payload, Signable.ID>(
            factorSourceId = deviceFactorSource1.id,
            perTransaction = listOf(
                TransactionSignRequestInput(
                    payload = Signable.Payload.Transaction(CompiledTransactionIntent.sample()),
                    factorSourceId = deviceFactorSource1.id,
                    ownedFactorInstances = listOf(deviceFactorInstances[0])
                )
            ),
            invalidTransactionsIfNeglected = emptyList()
        ) to PerFactorOutcome(
            factorSourceId = deviceFactorSource1.id,
            outcome = FactorOutcome.Signed(
                producedSignatures = listOf(
                    HdSignature(
                        input = HdSignatureInput(
                            payloadId = Signable.ID.Transaction(CompiledTransactionIntent.sample().decompile().hash()),
                            ownedFactorInstance = deviceFactorInstances[0]
                        ),
                        signature = deviceMnemonic.sign(
                            hash = CompiledTransactionIntent.sample().decompile().hash().hash,
                            path = deviceFactorInstances[0].factorInstance.publicKey.derivationPath
                        )
                    )
                )
            )
        ),
        PerFactorSourceInput<Signable.Payload, Signable.ID>(
            factorSourceId = deviceFactorSource2.id,
            perTransaction = listOf(
                TransactionSignRequestInput(
                    payload = Signable.Payload.Transaction(CompiledTransactionIntent.sample()),
                    factorSourceId = deviceFactorSource2.id,
                    ownedFactorInstances = listOf(deviceFactorInstances[1])
                )
            ),
            invalidTransactionsIfNeglected = emptyList()
        ) to PerFactorOutcome(
            factorSourceId = deviceFactorSource2.id,
            outcome = FactorOutcome.Signed(
                producedSignatures = listOf(
                    HdSignature(
                        input = HdSignatureInput(
                            payloadId = Signable.ID.Transaction(CompiledTransactionIntent.sample().decompile().hash()),
                            ownedFactorInstance = deviceFactorInstances[1]
                        ),
                        signature = otherDeviceMnemonic.sign(
                            hash = CompiledTransactionIntent.sample().decompile().hash().hash,
                            path = deviceFactorInstances[1].factorInstance.publicKey.derivationPath
                        )
                    )
                )
            )
        ),
        PerFactorSourceInput<Signable.Payload, Signable.ID>(
            factorSourceId = ledgerFactorSource1.id,
            perTransaction = listOf(
                TransactionSignRequestInput(
                    payload = Signable.Payload.Transaction(CompiledTransactionIntent.sample()),
                    factorSourceId = ledgerFactorSource1.id,
                    ownedFactorInstances = listOf(ledgerFactorInstances[0])
                )
            ),
            invalidTransactionsIfNeglected = emptyList()
        ) to PerFactorOutcome(
            factorSourceId = ledgerFactorSource1.id,
            outcome = FactorOutcome.Signed(
                producedSignatures = listOf(
                    HdSignature(
                        input = HdSignatureInput(
                            payloadId = Signable.ID.Transaction(CompiledTransactionIntent.sample().decompile().hash()),
                            ownedFactorInstance = ledgerFactorInstances[0]
                        ),
                        signature = fakeLedgerMnemonic.sign(
                            hash = CompiledTransactionIntent.sample().decompile().hash().hash,
                            path = ledgerFactorInstances[0].factorInstance.publicKey.derivationPath
                        )
                    )
                )
            )
        )
    )

    @Test
    fun `when one device factor source is received, mono signing is resolved`() = testScope.runTest {
        coEvery { accessDeviceFactorSource.signMono(any(), any()) } coAnswers {
            delay(50)
            Result.success(
                signaturesPerInput.values.first()
            )
        }

        val input = AccessFactorSourcesInput.ToSign(
            purpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
            kind = FactorSourceKind.DEVICE,
            input = signaturesPerInput.keys.first()
        )

        val vm = initVM(input = input)
        vm.state.test {
            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    accessState = AccessFactorSourceDelegate.State(
                        factorSourceToAccess = AccessFactorSourceDelegate.State.FactorSourcesToAccess.Resolving(
                            id = deviceFactorSource1.id.asGeneral()
                        )
                    )
                ),
                awaitItem()
            )
            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    accessState = AccessFactorSourceDelegate.State(
                        isAccessInProgress = true,
                        factorSourceToAccess = AccessFactorSourceDelegate.State.FactorSourcesToAccess.Mono(
                            factorSource = deviceFactorSource1.asGeneral()
                        )
                    )
                ),
                awaitItem()
            )
            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    accessState = AccessFactorSourceDelegate.State(
                        isAccessInProgress = false,
                        factorSourceToAccess = AccessFactorSourceDelegate.State.FactorSourcesToAccess.Mono(
                            factorSource = deviceFactorSource1.asGeneral()
                        )
                    )
                ),
                awaitItem()
            )
            ensureAllEventsConsumed()
            coVerify {
                accessFactorSourcesIOHandler.setOutput(
                    AccessFactorSourcesOutput.SignOutput.Completed(
                        outcome = signaturesPerInput.values.toList()[0]
                    )
                )
            }
        }
    }

    @Test
    fun `when one ledger factor source is received, mono signing is resolved`() = testScope.runTest {
        coEvery { accessLedgerHardwareWalletFactorSource.signMono(any(), any()) } coAnswers {
            delay(50)
            Result.success(signaturesPerInput.values.toList()[2])
        }
        val input = AccessFactorSourcesInput.ToSign(
            purpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
            kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
            input = signaturesPerInput.keys.toList()[2]
        )

        val vm = initVM(input = input)
        vm.state.test {
            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    accessState = AccessFactorSourceDelegate.State(
                        factorSourceToAccess = AccessFactorSourceDelegate.State.FactorSourcesToAccess.Resolving(
                            id = ledgerFactorSource1.id.asGeneral()
                        )
                    )
                ),
                awaitItem()
            )

            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    accessState = AccessFactorSourceDelegate.State(
                        isAccessInProgress = true,
                        factorSourceToAccess = AccessFactorSourceDelegate.State.FactorSourcesToAccess.Mono(
                            factorSource = ledgerFactorSource1.asGeneral()
                        )
                    )
                ),
                awaitItem()
            )

            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    accessState = AccessFactorSourceDelegate.State(
                        isAccessInProgress = false,
                        factorSourceToAccess = AccessFactorSourceDelegate.State.FactorSourcesToAccess.Mono(
                            factorSource = ledgerFactorSource1.asGeneral()
                        )
                    )
                ),
                awaitItem()
            )

            coVerify {
                accessFactorSourcesIOHandler.setOutput(
                    output = AccessFactorSourcesOutput.SignOutput.Completed(
                        outcome = signaturesPerInput.values.toList()[2]
                    )
                )
            }
        }
    }

    private fun <SP : Signable.Payload, ID : Signable.ID> initVM(input: AccessFactorSourcesInput.ToSign<SP, ID>): GetSignaturesViewModel {
        every { accessFactorSourcesIOHandler.getInput() } returns input
        coEvery { accessFactorSourcesIOHandler.setOutput(any()) } just Runs

        return GetSignaturesViewModel(
            accessFactorSourcesIOHandler = accessFactorSourcesIOHandler,
            accessDeviceFactorSource = accessDeviceFactorSource,
            accessLedgerHardwareWalletFactorSource = accessLedgerHardwareWalletFactorSource,
            accessOffDeviceMnemonicFactorSource = accessOffDeviceMnemonicFactorSource,
            accessArculusFactorSourceUseCase = accessArculusFactorSourceUseCase,
            accessPasswordFactorSourceUseCase = accessPasswordFactorSourceUseCase,
            defaultDispatcher = testDispatcher,
            getProfileUseCase = GetProfileUseCase(
                profileRepository = FakeProfileRepository(sampleProfile),
                dispatcher = testDispatcher
            )
        )
    }
}

