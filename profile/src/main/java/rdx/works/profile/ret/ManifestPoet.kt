package rdx.works.profile.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.ManifestBuilder
import com.radixdlt.ret.ManifestBuilderBucket
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.nonFungibleLocalIdFromStr
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import java.math.BigDecimal
import java.math.RoundingMode

object ManifestPoet {

    fun buildRola(
        entityAddress: String,
        publicKeyHashes: List<FactorInstance.PublicKey>
    ) = BabylonManifestBuilder()
        .setOwnerKeys(entityAddress, publicKeyHashes)
        .buildSafely(RetBridge.Address.networkId(entityAddress))

    fun buildFaucet(toAddress: String) = BabylonManifestBuilder()
        .lockFee()
        .freeXrd()
        .accountTryDepositEntireWorktopOrAbort(toAddress = toAddress)
        .buildSafely(RetBridge.Address.networkId(toAddress))


    fun buildTransfer(
        fromAccount: Network.Account,
        depositFungibles: List<FungibleTransfer>,
        depositNFTs: List<NonFungibleTransfer>
    ) = BabylonManifestBuilder()
        .attachInstructions(
            fromAccount = fromAccount,
            depositFungibles = depositFungibles
        ).attachInstructions(
            fromAccount = fromAccount,
            depositNFTs = depositNFTs
        ).buildSafely(
            networkId = RetBridge.Address.networkId(fromAccount.address)
        )

    fun buildClaim(
        fromAccount: Network.Account,
        claims: List<Claim>
    ): Result<TransactionManifest> {
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
            val xrdAddress = RetBridge.Address.xrdAddress(forNetworkId = fromAccount.networkID)

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
    ): Result<TransactionManifest> = BabylonManifestBuilder().apply {
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
        RetBridge.Address.networkId(settings.accountAddress)
    )

    private fun BabylonManifestBuilder.attachInstructions(
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

    private fun BabylonManifestBuilder.attachInstructions(
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