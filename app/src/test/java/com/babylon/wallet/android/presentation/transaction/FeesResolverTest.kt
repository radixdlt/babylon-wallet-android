package com.babylon.wallet.android.presentation.transaction

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.transaction.analysis.FeesResolver
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.FeeLocks
import com.radixdlt.ret.FeeSummary
import com.radixdlt.ret.NewEntities
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.formattedPlain
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import rdx.works.profile.ret.crypto.PrivateKey

class FeesResolverTest {

    private val emptyExecutionSummary = ExecutionSummary(
        feeLocks = FeeLocks(
            lock = Decimal.zero(),
            contingentLock = Decimal.zero()
        ),
        feeSummary = FeeSummary(
            executionCost = Decimal.zero(),
            finalizationCost = Decimal.zero(),
            storageExpansionCost = Decimal.zero(),
            royaltyCost = Decimal.zero()
        ),
        detailedClassification = listOf(),
        reservedInstructions = listOf(),
        accountDeposits = mapOf(),
        accountsRequiringAuth = listOf(),
        accountWithdraws = mapOf(),
        encounteredEntities = listOf(),
        identitiesRequiringAuth = listOf(),
        newEntities = NewEntities(
            componentAddresses = listOf(),
            resourceAddresses = listOf(),
            packageAddresses = listOf(),
            metadata = mapOf()
        ),
        presentedProofs = mapOf(),
        newlyCreatedNonFungibles = listOf()
    )
    private val notaryAndSigners = NotaryAndSigners(
        listOf(),
        PrivateKey.EddsaEd25519.newRandom()
    )

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 1`() {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("0.9"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = FeesResolver.resolve(summary, notaryAndSigners, PreviewType.None)

        assertEquals("0.1283795", fees.networkFeeDisplayed)
        assertEquals("0.2", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.3283795", fees.defaultTransactionFee.formatted())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 2`() = runTest {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("0.5"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = FeesResolver.resolve(summary, notaryAndSigners, PreviewType.None)

        assertEquals("0.5283795", fees.networkFeeDisplayed)
        assertEquals("0.2", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.7283795", fees.defaultTransactionFee.formatted())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 3`() = runTest {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("1.0"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = FeesResolver.resolve(summary, notaryAndSigners, PreviewType.None)

        assertEquals("0.0283795", fees.networkFeeDisplayed)
        assertEquals("0.2", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.2283795", fees.defaultTransactionFee.formatted())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 4`() = runTest {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal("1.5"),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = FeesResolver.resolve(summary, notaryAndSigners, PreviewType.None)

        assertNull(fees.networkFeeDisplayed)
        assertEquals("0", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("0", fees.defaultTransactionFee.formattedPlain())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 4 with one signer account`() = runTest {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = Decimal.zero(),
                contingentLock = Decimal.zero()
            ),
            feeSummary = FeeSummary(
                executionCost = Decimal("0.3"),
                finalizationCost = Decimal("0.3"),
                storageExpansionCost = Decimal("0.2"),
                royaltyCost = Decimal("0.2")
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = FeesResolver.resolve(
            summary = summary,
            notaryAndSigners = notaryAndSigners.copy(
                signers = listOf(
                    SampleDataProvider().sampleAccount(
                        address = "rdx_t_12382918379821",
                        name = "Savings account"
                    )
                )
            ),
            previewType = PreviewType.None
        )


        assertEquals("1.040813", fees.networkFeeDisplayed)
        assertEquals("0.2", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("1.240813", fees.defaultTransactionFee.formatted())
    }
}