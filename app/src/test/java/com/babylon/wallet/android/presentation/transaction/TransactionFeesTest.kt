package com.babylon.wallet.android.presentation.transaction

import com.babylon.wallet.android.DefaultLocaleRule
import com.babylon.wallet.android.domain.usecases.signing.NotaryAndSigners
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.FeeLocks
import com.radixdlt.sargon.FeeSummary
import com.radixdlt.sargon.NewEntities
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.asProfileEntity
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.formattedPlain
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TransactionFeesTest {

    @get:Rule
    val defaultLocaleTestRule = DefaultLocaleRule()

    private val emptyExecutionSummary = ExecutionSummary(
        feeLocks = FeeLocks(
            lock = 0.toDecimal192(),
            contingentLock = 0.toDecimal192()
        ),
        feeSummary = FeeSummary(
            executionCost = 0.toDecimal192(),
            finalizationCost = 0.toDecimal192(),
            storageExpansionCost = 0.toDecimal192(),
            royaltyCost = 0.toDecimal192()
        ),
        detailedClassification = listOf(),
        reservedInstructions = listOf(),
        deposits = mapOf(),
        withdrawals = mapOf(),
        addressesOfAccountsRequiringAuth = listOf(),
        addressesOfIdentitiesRequiringAuth = listOf(),
        encounteredAddresses = listOf(),
        newEntities = NewEntities(
            metadata = mapOf()
        ),
        presentedProofs = listOf(),
        newlyCreatedNonFungibles = listOf()
    )
    private val notaryAndSigners = NotaryAndSigners(
        listOf(),
        Curve25519SecretKey.secureRandom()
    )

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 1`() {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = 0.9.toDecimal192(),
                contingentLock = 0.toDecimal192()
            ),
            feeSummary = FeeSummary(
                executionCost = 0.3.toDecimal192(),
                finalizationCost = 0.3.toDecimal192(),
                storageExpansionCost = 0.2.toDecimal192(),
                royaltyCost = 0.2.toDecimal192()
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = TransactionFees.from(summary, notaryAndSigners, PreviewType.None)

        assertEquals("0.1283795", fees.networkFeeDisplayed)
        assertEquals("0.2", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.3283795", fees.defaultTransactionFee.formatted())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 2`() = runTest {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = 0.5.toDecimal192(),
                contingentLock = 0.toDecimal192()
            ),
            feeSummary = FeeSummary(
                executionCost = 0.3.toDecimal192(),
                finalizationCost = 0.3.toDecimal192(),
                storageExpansionCost = 0.2.toDecimal192(),
                royaltyCost = 0.2.toDecimal192()
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = TransactionFees.from(summary, notaryAndSigners, PreviewType.None)

        assertEquals("0.5283795", fees.networkFeeDisplayed)
        assertEquals("0.2", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.7283795", fees.defaultTransactionFee.formatted())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 3`() = runTest {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = 1.0.toDecimal192(),
                contingentLock = 0.toDecimal192()
            ),
            feeSummary = FeeSummary(
                executionCost = 0.3.toDecimal192(),
                finalizationCost = 0.3.toDecimal192(),
                storageExpansionCost = 0.2.toDecimal192(),
                royaltyCost = 0.2.toDecimal192()
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = TransactionFees.from(summary, notaryAndSigners, PreviewType.None)

        assertEquals("0.0283795", fees.networkFeeDisplayed)
        assertEquals("0.2", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("0.2283795", fees.defaultTransactionFee.formatted())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 4`() = runTest {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = 1.5.toDecimal192(),
                contingentLock = 0.toDecimal192()
            ),
            feeSummary = FeeSummary(
                executionCost = 0.3.toDecimal192(),
                finalizationCost = 0.3.toDecimal192(),
                storageExpansionCost = 0.2.toDecimal192(),
                royaltyCost = 0.2.toDecimal192()
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = TransactionFees.from(summary, notaryAndSigners, PreviewType.None)

        assertNull(fees.networkFeeDisplayed)
        assertEquals("0", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("0", fees.defaultTransactionFee.formattedPlain())
    }

    @Test
    fun `verify network fee royalty and total fee is displayed correctly on default screen 4 with one signer account`() = runTest {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = 0.toDecimal192(),
                contingentLock = 0.toDecimal192()
            ),
            feeSummary = FeeSummary(
                executionCost = 0.3.toDecimal192(),
                finalizationCost = 0.3.toDecimal192(),
                storageExpansionCost = 0.2.toDecimal192(),
                royaltyCost = 0.2.toDecimal192()
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = TransactionFees.from(
            summary = summary,
            notaryAndSigners = notaryAndSigners.copy(
                signers = listOf(Account.sampleMainnet().asProfileEntity())
            ),
            previewType = PreviewType.None
        )


        assertEquals("1.040813", fees.networkFeeDisplayed)
        assertEquals("0.2", fees.defaultRoyaltyFeesDisplayed)
        assertEquals("1.240813", fees.defaultTransactionFee.formatted())
    }

    @Test
    fun `verify total transaction fee to lock is zero when fully paid by dapp`() = runTest {
        val summary = emptyExecutionSummary.copy(
            feeLocks = FeeLocks(
                lock = 1.1.toDecimal192(),
                contingentLock = 0.toDecimal192()
            ),
            feeSummary = FeeSummary(
                executionCost = 0.20627775.toDecimal192(),
                finalizationCost = 0.01525175.toDecimal192(),
                storageExpansionCost = 0.0343322748.toDecimal192(),
                royaltyCost = 0.toDecimal192()
            ),
            detailedClassification = listOf(
                DetailedManifestClass.General
            ),
            reservedInstructions = emptyList()
        )

        val fees = TransactionFees.from(
            summary = summary,
            notaryAndSigners = notaryAndSigners.copy(
                signers = listOf(Account.sampleMainnet().asProfileEntity())
            ),
            previewType = PreviewType.None
        )

        assertEquals("0.2255169", fees.totalExecutionCostDisplayed)
        assertEquals("0.0152518", fees.finalizationCostDisplayed)
        assertEquals("0.0343323", fees.storageExpansionCostDisplayed)
        assertEquals("0.041265137517", fees.feePaddingAmountToDisplay)
        assertEquals("0", fees.transactionFeeToLock.formatted())
    }
}