package rdx.works.profile.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instruction
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestAddress
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.TransactionManifest
import rdx.works.profile.ret.transaction.TransactionManifestData
import java.math.BigDecimal
import java.math.RoundingMode

fun TransactionManifestData.addLockFee(
    feePayerAddress: String,
    fee: BigDecimal
): TransactionManifestData {
    val manifest = toTransactionManifest().getOrThrow()

    return TransactionManifestData.from(
        manifest = TransactionManifest(
            instructions = Instructions.fromInstructions(
                instructions = listOf(
                    Instruction.CallMethod(
                        address = ManifestAddress.Static(Address(feePayerAddress)),
                        methodName = ManifestMethod.LockFee.value,
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
    val manifest = toTransactionManifest().getOrThrow()

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