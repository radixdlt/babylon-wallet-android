package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.manifest.toPrettyString
import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatorsUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppInTransactionUseCase
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNFTDetailsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.guaranteesCount
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.TransactionManifest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.then
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.currentNetwork
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getValidatorsUseCase: GetValidatorsUseCase,
    private val cacheNewlyCreatedEntitiesUseCase: CacheNewlyCreatedEntitiesUseCase,
    private val getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    private val getNFTDetailsUseCase: GetNFTDetailsUseCase,
    private val resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase,
    private val searchFeePayersUseCase: SearchFeePayersUseCase
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionAnalysis")

    suspend fun analyse(transactionClient: TransactionClient) {
        _state.value.requestNonNull.transactionManifestData
            .toTransactionManifest()
            .then {
                startAnalysis(it, transactionClient)
            }.onFailure { error ->
                reportFailure(error)
            }
    }

    private suspend fun startAnalysis(
        manifest: TransactionManifest,
        transactionClient: TransactionClient
    ): Result<Unit> {
        val networkId = getProfileUseCase().first().currentNetwork?.knownNetworkId ?: error("No network found")
        val notaryAndSigners = transactionClient.getNotaryAndSigners(
            manifest = manifest,
            ephemeralNotaryPrivateKey = _state.value.ephemeralNotaryPrivateKey
        )
        manifest.toPrettyString().let { logger.d(it) }
        return transactionClient.getTransactionPreview(
            manifest = manifest,
            notaryAndSigners = notaryAndSigners
        ).mapCatching { preview ->
            Timber.tag("receipt").d(preview.encodedReceipt)
            manifest
                .executionSummary(networkId = networkId.value.toUByte(), encodedReceipt = preview.encodedReceipt.decodeHex())
                .resolvePreview(notaryAndSigners)
                .resolveFees(notaryAndSigners)
        }.mapCatching { transactionFees ->
            val feePayerResult = searchFeePayersUseCase(
                manifest = manifest,
                lockFee = transactionFees.defaultTransactionFee
            ).getOrThrow()

            _state.update {
                it.copy(
                    isNetworkFeeLoading = false,
                    transactionFees = transactionFees,
                    feePayerSearchResult = feePayerResult
                )
            }
        }
    }

    private suspend fun ExecutionSummary.resolvePreview(notaryAndSigners: NotaryAndSigners) = apply {
        val previewType = if (_state.value.requestNonNull.isInternal.not() && reservedInstructions.isNotEmpty()) {
            // wallet unacceptable manifest
            _state.update {
                it.copy(
                    error = UiMessage.TransactionErrorMessage(RadixWalletException.DappRequestException.UnacceptableManifest)
                )
            }
            PreviewType.UnacceptableManifest
        } else if (detailedClassification.isEmpty()) {
            PreviewType.NonConforming
        } else {
            processConformingManifest()
        }

        if (previewType is PreviewType.Transfer) {
            val newlyCreated = previewType.getNewlyCreatedResources()
            if (newlyCreated.isNotEmpty()) {
                cacheNewlyCreatedEntitiesUseCase(newlyCreated.map { it.resource })
            }
        }

        _state.update {
            it.copy(
                isRawManifestVisible = previewType == PreviewType.NonConforming,
                isLoading = false,
                previewType = previewType,
                defaultSignersCount = notaryAndSigners.signers.count()
            )
        }
    }

    private fun ExecutionSummary.resolveFees(notaryAndSigners: NotaryAndSigners) = TransactionFees(
        nonContingentFeeLock = feeLocks.lock.asStr().toBigDecimal(),
        networkExecution = feeSummary.executionCost.asStr().toBigDecimal(),
        networkFinalization = feeSummary.finalizationCost.asStr().toBigDecimal(),
        networkStorage = feeSummary.storageExpansionCost.asStr().toBigDecimal(),
        royalties = feeSummary.royaltyCost.asStr().toBigDecimal(),
        guaranteesCount = (_state.value.previewType as? PreviewType.Transfer)?.to?.guaranteesCount() ?: 0,
        notaryIsSignatory = notaryAndSigners.notaryIsSignatory,
        includeLockFee = false, // First its false because we don't know if lock fee is applicable or not yet
        signersCount = notaryAndSigners.signers.count()
    ).let { fees ->
        if (fees.defaultTransactionFee > BigDecimal.ZERO) {
            // There will be a lock fee so update lock fee cost
            fees.copy(includeLockFee = true)
        } else {
            fees
        }
    }

    private suspend fun ExecutionSummary.processConformingManifest(): PreviewType {
        val networkId = requireNotNull(getProfileUseCase.currentNetwork()?.knownNetworkId)
        val xrdAddress = XrdResource.address(networkId)
        val transactionType = detailedClassification.first()
        val resources = getResourcesUseCase(addresses = involvedResourceAddresses + xrdAddress).getOrThrow()

        return when (transactionType) {
            is DetailedManifestClass.General -> transactionType.resolve(
                resources = resources,
                getTransactionBadgesUseCase = getTransactionBadgesUseCase,
                getProfileUseCase = getProfileUseCase,
                resolveDAppInTransactionUseCase = resolveDAppInTransactionUseCase,
                executionSummary = this
            )

            is DetailedManifestClass.AccountDepositSettingsUpdate -> transactionType.resolve(
                getProfileUseCase = getProfileUseCase,
                allResources = resources
            )

            is DetailedManifestClass.Transfer -> resolveTransfer(getProfileUseCase, resources)
            is DetailedManifestClass.ValidatorClaim -> {
                val validators = getValidatorsUseCase(transactionType.involvedValidatorAddresses).getOrThrow()
                val involvedStakeClaims = involvedStakeClaims
                val stakeClaimsNfts = involvedStakeClaims.map {
                    getNFTDetailsUseCase(it.resourceAddress, it.localId).getOrDefault(emptyList())
                }.flatten()
                transactionType.resolve(
                    executionSummary = this,
                    getProfileUseCase = getProfileUseCase,
                    resources = resources,
                    involvedValidators = validators,
                    stakeClaimsNfts = stakeClaimsNfts
                )
            }

            is DetailedManifestClass.ValidatorStake -> {
                val validators = getValidatorsUseCase(transactionType.involvedValidatorAddresses).getOrThrow()
                transactionType.resolve(
                    getProfileUseCase = getProfileUseCase,
                    resources = resources,
                    involvedValidators = validators,
                    executionSummary = this
                )
            }

            is DetailedManifestClass.ValidatorUnstake -> {
                val validators = getValidatorsUseCase(transactionType.involvedValidatorAddresses).getOrThrow()
                transactionType.resolve(
                    getProfileUseCase = getProfileUseCase,
                    resources = resources,
                    involvedValidators = validators,
                    executionSummary = this
                )
            }

            else -> PreviewType.NonConforming
        }
    }

    private fun reportFailure(error: Throwable) {
        logger.w(error)

        _state.update {
            it.copy(
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.None,
                error = UiMessage.TransactionErrorMessage(error)
            )
        }
    }
}
