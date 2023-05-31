package com.babylon.wallet.android.presentation.transfer.prepare

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.toTransactionRequest
import com.babylon.wallet.android.data.transaction.MethodName
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.ManifestAstValue
import com.radixdlt.toolkit.models.NonFungibleLocalIdInternal
import com.radixdlt.toolkit.models.ValueKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID

class PrepareManifestDelegate(
    private val state: MutableStateFlow<TransferViewModel.State>,
    private val incomingRequestRepository: IncomingRequestRepository
) {

    suspend fun onSubmit() {
        val fromAccount = state.value.fromAccount ?: return
        prepareRequest(fromAccount, state.value).onValue { request ->
            state.update { it.copy(transferRequestId = request.requestId) }
            Timber.d("Manifest for ${request.requestId} prepared:")
            Timber.d(request.transactionManifestData.instructions)
            incomingRequestRepository.add(request)
        }.onError { error ->
            state.update { it.copy(error = UiMessage.ErrorMessage(error)) }
        }
    }

    private fun prepareRequest(
        fromAccount: Network.Account,
        currentState: TransferViewModel.State
    ): Result<MessageFromDataChannel.IncomingRequest.TransactionRequest> {
        val manifest = ManifestBuilder()
            .attachInstructionsForFungibles(
                fromAccount = fromAccount,
                targetAccounts = currentState.targetAccounts
            )
            .attachInstructionsForNFTs(
                fromAccount = fromAccount,
                targetAccounts = currentState.targetAccounts
            )
            .build()

        return manifest.toTransactionRequest(
            networkId = fromAccount.networkID,
            message = currentState.submittedMessage,
        )
    }

    @Suppress("NestedBlockDepth")
    private fun ManifestBuilder.attachInstructionsForFungibles(
        fromAccount: Network.Account,
        targetAccounts: List<TargetAccount>
    ): ManifestBuilder = apply {
        state.value.withdrawingFungibles().forEach { (resource, amount) ->
            // Withdraw the total amount for each fungible
            addInstruction(withdraw(fromAccount = fromAccount, fungible = resource, amount = amount))

            // Deposit to each target account
            targetAccounts.filter { targetAccount ->
                targetAccount.assets.any { it.address == resource.resourceAddress }
            }.forEach { targetAccount ->
                val spendingFungibleAsset = targetAccount.assets.find { it.address == resource.resourceAddress } as? SpendingAsset.Fungible
                if (spendingFungibleAsset != null) {
                    val bucket = ManifestAstValue.Bucket(identifier = "${targetAccount.address}_${resource.resourceAddress}")

                    // First fill in a bucket from worktop with the correct amount
                    addInstruction(pourToBucket(fungible = resource, amount = spendingFungibleAsset.amountDecimal, bucket = bucket))

                    // Then deposit that bucket
                    addInstruction(deposit(bucket = bucket, into = targetAccount))
                }
            }
        }
    }

    private fun ManifestBuilder.attachInstructionsForNFTs(
        fromAccount: Network.Account,
        targetAccounts: List<TargetAccount>
    ): ManifestBuilder = apply {
        targetAccounts.forEach { targetAccount ->
            val nonFungibleSpendingAssets = targetAccount.assets.filterIsInstance<SpendingAsset.NFT>()
            nonFungibleSpendingAssets.forEach { nft ->
                val bucket = ManifestAstValue.Bucket(identifier = "${targetAccount.address}_${nft.address}")

                addInstruction(withdraw(fromAccount = fromAccount, nonFungible = nft.item))
                addInstruction(pourToBucket(nonFungible = nft.item, bucket = bucket))
                addInstruction(deposit(bucket = bucket, into = targetAccount))
            }
        }
    }

    /**
     * Sums all the amount needed to be withdrawn for each fungible
     */
    private fun TransferViewModel.State.withdrawingFungibles(): Map<Resource.FungibleResource, BigDecimal> {
        val allFungibles: List<SpendingAsset.Fungible> =
            targetAccounts.map { it.assets.filterIsInstance<SpendingAsset.Fungible>() }.flatten()

        val fungibleAmounts = mutableMapOf<Resource.FungibleResource, BigDecimal>()
        allFungibles.forEach { fungible ->
            val alreadySpentAmount = fungibleAmounts[fungible.resource] ?: BigDecimal.ZERO

            fungibleAmounts[fungible.resource] = alreadySpentAmount + fungible.amountDecimal
        }

        return fungibleAmounts
    }

    private fun withdraw(
        fromAccount: Network.Account,
        fungible: Resource.FungibleResource,
        amount: BigDecimal
    ) = Instruction.CallMethod(
        componentAddress = ManifestAstValue.Address(address = fromAccount.address),
        methodName = ManifestAstValue.String(MethodName.Withdraw.stringValue),
        arguments = arrayOf(
            ManifestAstValue.Address(fungible.resourceAddress),
            ManifestAstValue.Decimal(amount)
        )
    )

    private fun pourToBucket(
        fungible: Resource.FungibleResource,
        amount: BigDecimal,
        bucket: ManifestAstValue.Bucket
    ) = Instruction.TakeFromWorktopByAmount(
        resourceAddress = ManifestAstValue.Address(address = fungible.resourceAddress),
        amount = ManifestAstValue.Decimal(amount),
        intoBucket = bucket
    )

    private fun withdraw(
        fromAccount: Network.Account,
        nonFungible: Resource.NonFungibleResource.Item,
    ) = Instruction.CallMethod(
        componentAddress = ManifestAstValue.Address(address = fromAccount.address),
        methodName = ManifestAstValue.String(MethodName.WithdrawNonFungibles.stringValue),
        arguments = arrayOf(
            ManifestAstValue.Address(nonFungible.collectionAddress),
            ManifestAstValue.Array(
                elementKind = ValueKind.NonFungibleLocalId,
                elements = arrayOf(nonFungible.toManifestLocalId())
            )
        )
    )

    private fun pourToBucket(
        nonFungible: Resource.NonFungibleResource.Item,
        bucket: ManifestAstValue.Bucket
    ) = Instruction.TakeFromWorktopByIds(
        resourceAddress = ManifestAstValue.Address(nonFungible.collectionAddress),
        ids = setOf(nonFungible.toManifestLocalId()),
        intoBucket = bucket
    )

    private fun deposit(
        bucket: ManifestAstValue.Bucket,
        into: TargetAccount
    ) = Instruction.CallMethod(
        componentAddress = ManifestAstValue.Address(address = into.address),
        methodName = ManifestAstValue.String(MethodName.Deposit.stringValue),
        arguments = arrayOf(bucket)
    )
}

// TO BE REMOVED AFTER KET IS COMPATIBLE WITH ASH
// This will be obsolete when the KET is compatible with ASH since there will be a generic constructor for Local Ids
// that takes a string and internally infers its type.
@Suppress("UnsafeCallOnNullableType")
private fun Resource.NonFungibleResource.Item.toManifestLocalId(): ManifestAstValue.NonFungibleLocalId = run {
    if (nftLocalIdStringRegex.matches(localId)) {
        val (stringId) = nftLocalIdStringRegex.find(localId)!!.destructured
        ManifestAstValue.NonFungibleLocalId(NonFungibleLocalIdInternal.String(stringId))
    } else if (nftLocalIdIntegerRegex.matches(localId)) {
        val (intId) = nftLocalIdIntegerRegex.find(localId)!!.destructured
        ManifestAstValue.NonFungibleLocalId(NonFungibleLocalIdInternal.Integer(intId.toULong()))
    } else if (nftLocalIdHexRegex.matches(localId)) {
        val (hexId) = nftLocalIdHexRegex.find(localId)!!.destructured
        ManifestAstValue.NonFungibleLocalId(NonFungibleLocalIdInternal.Bytes(hexId.toByteArray()))
    } else if (nftLocalIdUUIDRegex.matches(localId)) {
        val (uuidString) = nftLocalIdUUIDRegex.find(localId)!!.destructured
        val uuid = UUID.fromString(uuidString)
        ManifestAstValue.NonFungibleLocalId(uuid.toLocalIdInternal())
    } else {
        error("Could not recognize id $localId")
    }
}

// https://gist.github.com/drmalex07/9008c611ffde6cb2ef3a2db8668bc251
@Suppress("MagicNumber")
private fun UUID.toLocalIdInternal(): NonFungibleLocalIdInternal {
    val shl = BigInteger.ONE.shiftLeft(64)
    var lo = BigInteger.valueOf(leastSignificantBits)
    var hi = BigInteger.valueOf(mostSignificantBits)

    if (hi.signum() < 0) {
        hi = hi.add(shl)
    }

    if (lo.signum() < 0) {
        lo = lo.add(shl)
    }

    val integer = lo.add(hi.multiply(shl))
    return NonFungibleLocalIdInternal.UUID(integer.toString())
}

private val nftLocalIdStringRegex = "^<(([a-zA-Z]|_|\\d)+)>\$".toRegex()
private val nftLocalIdIntegerRegex = "^#(\\d+)#\$".toRegex()
private val nftLocalIdHexRegex = "^\\[([A-Fa-f\\d]+)]\$".toRegex()
private val nftLocalIdUUIDRegex = "^\\{(.+)\\}\$".toRegex()
