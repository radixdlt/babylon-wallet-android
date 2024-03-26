package rdx.works.profile.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instruction
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestAddress
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.AddressHelper
import rdx.works.profile.data.model.pernetwork.FactorInstance
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
