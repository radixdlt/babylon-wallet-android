package rdx.works.profile.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instruction
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestAddress
import com.radixdlt.ret.ManifestBuilder
import com.radixdlt.ret.ManifestBuilderBucket
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.nonFungibleLocalIdFromStr
import rdx.works.core.AddressHelper
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.ret.transaction.TransactionManifestData
import java.math.BigDecimal
import java.math.RoundingMode

object ManifestPoet {

    fun buildRola(
        entityAddress: String,
        publicKeyHashes: List<FactorInstance.PublicKey>
    ) = BabylonManifestBuilder()
        .setOwnerKeys(entityAddress, publicKeyHashes)
        .buildSafely(AddressHelper.networkId(entityAddress))

    fun buildFaucet(toAddress: String) = BabylonManifestBuilder()
        .lockFee()
        .freeXrd()
        .accountTryDepositEntireWorktopOrAbort(toAddress = toAddress)
        .buildSafely(AddressHelper.networkId(toAddress))


    fun buildTransfer(
        fromAccount: Network.Account,
        depositFungibles: List<FungibleTransfer>,
        depositNFTs: List<NonFungibleTransfer>
    ) = BabylonManifestBuilder()
        .attachInstructionsForFungibles(
            fromAccount = fromAccount,
            depositFungibles = depositFungibles
        ).attachInstructionsForNonFungibles(
            fromAccount = fromAccount,
            depositNFTs = depositNFTs
        ).buildSafely(
            networkId = AddressHelper.networkId(fromAccount.address)
        )

    fun buildClaim(
        fromAccount: Network.Account,
        claims: List<Claim>
    ): Result<TransactionManifestData> {
        var builder = ManifestBuilder()
        var bucketCounter = 0

        claims.forEach { claim ->
            val claimBucket = ManifestBuilderBucket("bucket$bucketCounter").also {
                bucketCounter += 1
            }

            val depositBucket = ManifestBuilderBucket("bucket$bucketCounter").also {
                bucketCounter += 1
            }

            val totalClaimValue = claim.claimNFTs.values.sumOf { it }
            val xrdAddress = AddressHelper.xrdAddress(forNetworkId = fromAccount.networkID)

            builder = builder
                .accountWithdrawNonFungibles(
                    address = Address(fromAccount.address),
                    resourceAddress = Address(claim.resourceAddress),
                    ids = claim.claimNFTs.keys.map { nonFungibleLocalIdFromStr(it) }
                ).takeAllFromWorktop(
                    resourceAddress = Address(claim.resourceAddress),
                    intoBucket = claimBucket
                ).validatorClaimXrd(
                    address = Address(claim.validatorAddress),
                    bucket = claimBucket
                ).takeFromWorktop(
                    resourceAddress = Address(xrdAddress),
                    amount = totalClaimValue.toRETDecimal(roundingMode = RoundingMode.HALF_DOWN),
                    intoBucket = depositBucket
                ).accountDeposit(
                    address = Address(fromAccount.address),
                    bucket = depositBucket
                )
        }

        return builder.buildSafely(fromAccount.networkID)
    }

    fun buildThirdPartyDeposits(
        settings: ThirdPartyDepositSettings
    ): Result<TransactionManifestData> = BabylonManifestBuilder().apply {
        if (settings.defaultDepositRule != null) {
            setDefaultDepositRule(
                accountAddress = settings.accountAddress,
                accountDefaultDepositRule = settings.defaultDepositRule
            )
        }

        settings.removeAssetExceptions.forEach { assetException ->
            removeResourcePreference(
                accountAddress = settings.accountAddress,
                resourceAddress = assetException.address
            )
        }

        settings.addAssetExceptions.forEach { assetException ->
            setResourcePreference(
                accountAddress = settings.accountAddress,
                resourceAddress = assetException.address,
                exceptionRule = assetException.exceptionRule
            )
        }

        settings.removeDepositors.forEach { depositorAddress ->
            removeAuthorizedDepositor(
                accountAddress = settings.accountAddress,
                depositorAddress = depositorAddress
            )
        }

        settings.addedDepositors.forEach { depositorAddress ->
            addAuthorizedDepositor(
                accountAddress = settings.accountAddress,
                depositorAddress = depositorAddress
            )
        }
    }.buildSafely(
        AddressHelper.networkId(settings.accountAddress)
    )

    private fun BabylonManifestBuilder.attachInstructionsForFungibles(
        fromAccount: Network.Account,
        depositFungibles: List<FungibleTransfer>
    ): BabylonManifestBuilder = apply {
        // calculate the withdraw sum of each fungible
        val withdraws = mutableMapOf<String, BigDecimal>()
        depositFungibles.forEach { trasfer ->
            val alreadySpentAmount = withdraws[trasfer.resourceAddress] ?: BigDecimal.ZERO
            withdraws[trasfer.resourceAddress] = alreadySpentAmount + trasfer.amount
        }

        withdraws.forEach { (withdrawingResource, totalWithdrawAmount) ->
            withdrawFromAccount(
                fromAddress = fromAccount.address,
                fungibleAddress = withdrawingResource,
                amount = totalWithdrawAmount
            )

            depositFungibles.filter { it.resourceAddress == withdrawingResource }.forEach { transfer ->
                val bucket = newBucket()

                takeFromWorktop(
                    fungibleAddress = transfer.resourceAddress,
                    amount = transfer.amount,
                    intoBucket = bucket
                )

                deposit(
                    toAccountAddress = transfer.toAccountAddress,
                    bucket = bucket,
                    isSignatureRequired = transfer.signatureRequired
                )
            }
        }
    }

    private fun BabylonManifestBuilder.attachInstructionsForNonFungibles(
        fromAccount: Network.Account,
        depositNFTs: List<NonFungibleTransfer>
    ): BabylonManifestBuilder = apply {
        depositNFTs.forEach { transfer ->
            val bucket = newBucket()

            withdrawNonFungiblesFromAccount(
                fromAddress = fromAccount.address,
                nonFungibleGlobalAddress = transfer.globalId
            )
            takeNonFungiblesFromWorktop(
                nonFungibleGlobalAddress = transfer.globalId,
                intoBucket = bucket
            )

            deposit(
                toAccountAddress = transfer.toAccountAddress,
                bucket = bucket,
                isSignatureRequired = transfer.signatureRequired,
            )
        }
    }

    private fun BabylonManifestBuilder.deposit(
        toAccountAddress: String,
        bucket: BabylonManifestBuilder.Bucket,
        isSignatureRequired: Boolean
    ): BabylonManifestBuilder = apply {
        if (isSignatureRequired) {
            accountDeposit(
                toAddress = toAccountAddress,
                fromBucket = bucket
            )
        } else {
            accountTryDepositOrAbort(
                toAddress = toAccountAddress,
                fromBucket = bucket
            )
        }
    }

    data class FungibleTransfer(
        val toAccountAddress: String,
        val resourceAddress: String,
        val amount: BigDecimal,
        val signatureRequired: Boolean
    )

    data class NonFungibleTransfer(
        val toAccountAddress: String,
        val globalId: String,
        val signatureRequired: Boolean
    )

    data class Claim(
        val resourceAddress: String,
        val validatorAddress: String,
        val claimNFTs: Map<String, BigDecimal>
    )

    data class ThirdPartyDepositSettings(
        val accountAddress: String,
        val defaultDepositRule: Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule?,
        val removeAssetExceptions: List<Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException>,
        val addAssetExceptions: List<Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException>,
        val removeDepositors: List<Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress>,
        val addedDepositors: List<Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress>
    )
}

fun sampleXRDWithdraw(
    fromAddress: String,
    value: BigDecimal
) = with(AddressHelper.networkId(fromAddress)) {
    BabylonManifestBuilder()
        .withdrawFromAccount(
            fromAddress = fromAddress,
            fungibleAddress = AddressHelper.xrdAddress(this),
            amount = value
        )
        .build(this)
}

fun TransactionManifestData.addLockFee(
    feePayerAddress: String,
    fee: BigDecimal
): TransactionManifestData {
    return TransactionManifestData.from(
        manifest = TransactionManifest(
            instructions = Instructions.fromInstructions(
                instructions = listOf(
                    Instruction.CallMethod(
                        address = ManifestAddress.Static(Address(feePayerAddress)),
                        methodName = "lock_fee",
                        args = ManifestValue.TupleValue(
                            fields = listOf(
                                ManifestValue.DecimalValue(fee.toRETDecimal(roundingMode = RoundingMode.HALF_UP))
                            )
                        )
                    )
                ) + manifest.instructions().instructionsList(),
                networkId = manifest.instructions().networkId()
            ),
            blobs = manifest.blobs()
        ),
        message = message
    )
}

fun TransactionManifestData.addGuaranteeInstructionToManifest(
    address: String,
    guaranteedAmount: BigDecimal,
    index: Int
): TransactionManifestData {
    return TransactionManifestData.from(
        manifest = TransactionManifest(
            instructions = Instructions.fromInstructions(
                instructions = manifest.instructions().instructionsList().toMutableList().apply {
                    add(
                        index = index,
                        element = Instruction.AssertWorktopContains(
                            resourceAddress = Address(address),
                            amount = guaranteedAmount.toRETDecimal(roundingMode = RoundingMode.HALF_UP)
                        )
                    )
                }.toList(),
                networkId = manifest.instructions().networkId()
            ),
            blobs = manifest.blobs()
        ),
        message = message
    )
}

@Suppress("MagicNumber")
internal fun BigDecimal.toRETDecimal(roundingMode: RoundingMode): Decimal = Decimal(setScale(18, roundingMode).toPlainString())
internal fun TransactionHeader.toPrettyString(): String = StringBuilder()
    .appendLine("[Start Epoch]         => $startEpochInclusive")
    .appendLine("[End Epoch]           => $endEpochExclusive")
    .appendLine("[Network id]          => $networkId")
    .appendLine("[Nonce]               => $nonce")
    .appendLine("[Notary is signatory] => $notaryIsSignatory")
    .appendLine("[Tip %]               => $tipPercentage")
    .toString()

internal fun TransactionManifest.toPrettyString(): String {
    val blobSeparator = "\n"
    val blobPreamble = "BLOBS\n"
    val blobLabel = "BLOB\n"

    val instructionsFormatted = instructions().asStr()

    val blobsByByteCount = blobs().mapIndexed { index, bytes ->
        "$blobLabel[$index]: #${bytes.size} bytes"
    }.joinToString(blobSeparator)

    val blobsString = if (blobsByByteCount.isNotEmpty()) {
        listOf(blobPreamble, blobsByByteCount).joinToString(separator = blobSeparator)
    } else {
        ""
    }

    return "$instructionsFormatted$blobsString"
}