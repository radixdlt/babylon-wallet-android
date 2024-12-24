package com.babylon.wallet.android.presentation

import app.cash.turbine.test
import com.babylon.wallet.android.domain.usecases.signing.SignWithDeviceFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignWithLedgerFactorSourceUseCase
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.GetSignaturesViewModel
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.HDSignatureInput
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.HdSignature
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.InputPerFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.InputPerTransaction
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.OutputPerFactorSource
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.SignaturesPerFactorSource
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
import com.radixdlt.sargon.newDeviceFactorSourceBabylon
import com.radixdlt.sargon.newMnemonicSampleDevice
import com.radixdlt.sargon.newMnemonicSampleDeviceOther
import com.radixdlt.sargon.newMnemonicSampleLedger
import com.radixdlt.sargon.samples.sample
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import rdx.works.core.sargon.Signable
import rdx.works.core.sargon.allAccountsOnCurrentNetwork
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.initBabylon
import rdx.works.core.sargon.transactionSigningFactorInstance
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
                        hdPublicKey = deviceMnemonic.derivePublicKey(DerivationPath.Account(
                            AccountPath(
                                networkId = NetworkId.MAINNET,
                                keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
                                index = Hardened.Unsecurified(v1 = UnsecurifiedHardened.initFromLocal(0u))
                            )
                        )),
                        factorSourceId = deviceFactorSource1.id.asGeneral(),
                    ),
                    Account.initBabylon(
                        networkId = NetworkId.MAINNET,
                        displayName = DisplayName("Acc2"),
                        hdPublicKey = otherDeviceMnemonic.derivePublicKey(DerivationPath.Account(
                            AccountPath(
                                networkId = NetworkId.MAINNET,
                                keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
                                index = Hardened.Unsecurified(v1 = UnsecurifiedHardened.initFromLocal(1u))
                            )
                        )),
                        factorSourceId = deviceFactorSource2.id.asGeneral(),
                    ),
                    Account.initBabylon(
                        networkId = NetworkId.MAINNET,
                        displayName = DisplayName("Acc3"),
                        hdPublicKey = fakeLedgerMnemonic.derivePublicKey(DerivationPath.Account(
                            AccountPath(
                                networkId = NetworkId.MAINNET,
                                keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
                                index = Hardened.Unsecurified(v1 = UnsecurifiedHardened.initFromLocal(2u))
                            )
                        )),
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
            factorInstance = it.securityState.transactionSigningFactorInstance
        )
    }
    private val deviceFactorInstances = ownedFactorInstances.filter { it.factorInstance.factorSourceId.kind == FactorSourceKind.DEVICE }
    private val ledgerFactorInstances = ownedFactorInstances.filter { it.factorInstance.factorSourceId.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET }

    private val signWithDeviceFactorSourceUseCaseMock = mockk<SignWithDeviceFactorSourceUseCase>()
    private val signWithLedgerFactorSourceUseCaseMock = mockk<SignWithLedgerFactorSourceUseCase>()
    private val accessFactorSourcesIOHandler = mockk<AccessFactorSourcesIOHandler>()

    private val signaturesPerInput: Map<InputPerFactorSource<Signable.Payload>, SignaturesPerFactorSource<Signable.ID>> = mapOf(
        InputPerFactorSource<Signable.Payload>(
            factorSourceId = deviceFactorSource1.id,
            transactions = listOf(InputPerTransaction(
                payload = Signable.Payload.Transaction(CompiledTransactionIntent.sample()),
                factorSourceId = deviceFactorSource1.id,
                ownedFactorInstances = listOf(deviceFactorInstances[0])
            ))
        ) to SignaturesPerFactorSource(
            factorSourceId = deviceFactorSource1.id,
            hdSignatures = listOf(HdSignature(
                input = HDSignatureInput(
                    payloadId = Signable.ID.Transaction(CompiledTransactionIntent.sample().decompile().hash()),
                    ownedFactorInstance = deviceFactorInstances[0]
                ),
                signature = deviceMnemonic.sign(
                    hash = CompiledTransactionIntent.sample().decompile().hash().hash,
                    path = deviceFactorInstances[0].factorInstance.publicKey.derivationPath
                )
            ))
        ),
        InputPerFactorSource<Signable.Payload>(
            factorSourceId = deviceFactorSource2.id,
            transactions = listOf(InputPerTransaction(
                payload = Signable.Payload.Transaction(CompiledTransactionIntent.sample()),
                factorSourceId = deviceFactorSource2.id,
                ownedFactorInstances = listOf(deviceFactorInstances[1])
            ))
        ) to SignaturesPerFactorSource(
            factorSourceId = deviceFactorSource2.id,
            hdSignatures = listOf(HdSignature(
                input = HDSignatureInput(
                    payloadId = Signable.ID.Transaction(CompiledTransactionIntent.sample().decompile().hash()),
                    ownedFactorInstance = deviceFactorInstances[1]
                ),
                signature = otherDeviceMnemonic.sign(
                    hash = CompiledTransactionIntent.sample().decompile().hash().hash,
                    path = deviceFactorInstances[1].factorInstance.publicKey.derivationPath
                )
            ))
        ),
        InputPerFactorSource<Signable.Payload>(
            factorSourceId = ledgerFactorSource1.id,
            transactions = listOf(InputPerTransaction(
                payload = Signable.Payload.Transaction(CompiledTransactionIntent.sample()),
                factorSourceId = ledgerFactorSource1.id,
                ownedFactorInstances = listOf(ledgerFactorInstances[0])
            ))
        ) to SignaturesPerFactorSource(
            factorSourceId = ledgerFactorSource1.id,
            hdSignatures = listOf(HdSignature(
                input = HDSignatureInput(
                    payloadId = Signable.ID.Transaction(CompiledTransactionIntent.sample().decompile().hash()),
                    ownedFactorInstance = ledgerFactorInstances[0]
                ),
                signature = fakeLedgerMnemonic.sign(
                    hash = CompiledTransactionIntent.sample().decompile().hash().hash,
                    path = ledgerFactorInstances[0].factorInstance.publicKey.derivationPath
                )
            ))
        )
    )

    @Test
    fun `when multiple device factor sources are received, poly signing is resolved`() = testScope.runTest {
        coEvery { signWithDeviceFactorSourceUseCaseMock.poly(any(), any()) } returns Result.success(
            signaturesPerInput.values.filter { it.factorSourceId.kind == FactorSourceKind.DEVICE }.toList()
        )
        val input = AccessFactorSourcesInput.ToSign(
            purpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
            kind = FactorSourceKind.DEVICE,
            perFactorSource = signaturesPerInput.keys.filter { it.factorSourceId.kind == FactorSourceKind.DEVICE }.toList()
        )

        val vm = initVM(input = input)
        vm.state.test {
            val initialState = awaitItem()
            assertEquals(
                GetSignaturesViewModel.State(signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents),
                initialState
            )

            val gatherFactorSourcesState = awaitItem()
            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    isSigningInProgress = true,
                    factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Poly(
                        kind = FactorSourceKind.DEVICE,
                        factorSources = listOf(
                            deviceFactorSource1.asGeneral(),
                            deviceFactorSource2.asGeneral()
                        )
                    )
                ),
                gatherFactorSourcesState
            )

            val returnSignaturesState = awaitItem()
            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    isSigningInProgress = false,
                    factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Poly(
                        kind = FactorSourceKind.DEVICE,
                        factorSources = listOf(
                            deviceFactorSource1.asGeneral(),
                            deviceFactorSource2.asGeneral()
                        )
                    )
                ),
                returnSignaturesState
            )

            coVerify {
                accessFactorSourcesIOHandler.setOutput(
                    AccessFactorSourcesOutput.SignOutput(
                        signaturesPerInput.values.filter { it.factorSourceId.kind == FactorSourceKind.DEVICE }.map {
                            OutputPerFactorSource.Signed(it)
                        }
                    )
                )
            }
        }
    }

    @Test
    fun `when one device factor source is received, mono signing is resolved`() = testScope.runTest {
        coEvery { signWithDeviceFactorSourceUseCaseMock.mono(any(), any()) } returns Result.success(
            signaturesPerInput.values.first()
        )

        val input = AccessFactorSourcesInput.ToSign(
            purpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
            kind = FactorSourceKind.DEVICE,
            perFactorSource = listOf(signaturesPerInput.keys.first())
        )

        val vm = initVM(input = input)
        vm.state.test {
            assertEquals(
                GetSignaturesViewModel.State(signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents),
                awaitItem()
            )
            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    isSigningInProgress = true,
                    factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Mono(
                        factorSource = deviceFactorSource1.asGeneral()
                    )
                ),
                awaitItem()
            )
            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    isSigningInProgress = false,
                    factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Mono(
                        factorSource = deviceFactorSource1.asGeneral()
                    )
                ),
                awaitItem()
            )

            coVerify {
                accessFactorSourcesIOHandler.setOutput(
                    AccessFactorSourcesOutput.SignOutput(
                        listOf(OutputPerFactorSource.Signed(signaturesPerInput.values.toList()[0]))
                    )
                )
            }
        }
    }

    @Test
    fun `when one ledger factor source is received, mono signing is resolved`() = testScope.runTest {
        coEvery { signWithLedgerFactorSourceUseCaseMock.mono(any(), any()) } returns Result.success(
            signaturesPerInput.values.toList()[2]
        )
        val input = AccessFactorSourcesInput.ToSign(
            purpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
            kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
            perFactorSource = listOf(signaturesPerInput.keys.toList()[2])
        )

        val vm = initVM(input = input)
        vm.state.test {
            assertEquals(
                GetSignaturesViewModel.State(signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents),
                awaitItem()
            )

            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    isSigningInProgress = true,
                    factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Mono(
                        factorSource = ledgerFactorSource1.asGeneral()
                    )
                ),
                awaitItem()
            )

            assertEquals(
                GetSignaturesViewModel.State(
                    signPurpose = AccessFactorSourcesInput.ToSign.Purpose.TransactionIntents,
                    isSigningInProgress = false,
                    factorSourcesToSign = GetSignaturesViewModel.State.FactorSourcesToSign.Mono(
                        factorSource = ledgerFactorSource1.asGeneral()
                    )
                ),
                awaitItem()
            )

            coVerify {
                accessFactorSourcesIOHandler.setOutput(
                    AccessFactorSourcesOutput.SignOutput(
                        listOf(OutputPerFactorSource.Signed(signaturesPerInput.values.toList()[2]))
                    )
                )
            }
        }
    }

    private fun <SP: Signable.Payload> initVM(input: AccessFactorSourcesInput.ToSign<SP>): GetSignaturesViewModel {
        every { accessFactorSourcesIOHandler.getInput() } returns input
        coEvery { accessFactorSourcesIOHandler.setOutput(any()) } just Runs

        return GetSignaturesViewModel(
            accessFactorSourcesIOHandler = accessFactorSourcesIOHandler,
            signWithDeviceFactorSourceUseCase = signWithDeviceFactorSourceUseCaseMock,
            signWithLedgerFactorSourceUseCase = signWithLedgerFactorSourceUseCaseMock,
            getProfileUseCase = GetProfileUseCase(
                profileRepository = FakeProfileRepository(sampleProfile),
                dispatcher = testDispatcher
            )
        )
    }
}

