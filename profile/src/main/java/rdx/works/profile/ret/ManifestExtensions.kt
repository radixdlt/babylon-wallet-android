package rdx.works.profile.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instruction
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestAddress
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.TransactionManifest
import java.math.BigDecimal
import java.math.RoundingMode

fun TransactionManifest.addLockFeeInstructionToManifest(
    addressToLockFee: String,
    fee: BigDecimal
): TransactionManifest = TransactionManifest(
    instructions = Instructions.fromInstructions(
        instructions = listOf(lockFeeInstruction(addressToLockFee, fee)) + instructions().instructionsList(),
        networkId = instructions().networkId()
    ),
    blobs = blobs()
)

private fun lockFeeInstruction(
    addressToLockFee: String,
    fee: BigDecimal
): Instruction {
    return Instruction.CallMethod(
        address = ManifestAddress.Static(Address(addressToLockFee)),
        methodName = ManifestMethod.LockFee.value,
        args = ManifestValue.TupleValue(
            fields = listOf(
                ManifestValue.DecimalValue(fee.toRETDecimal(roundingMode = RoundingMode.HALF_UP))
            )
        )
    )
}

fun TransactionManifest.addGuaranteeInstructionToManifest(
    address: String,
    guaranteedAmount: BigDecimal,
    index: Int
): TransactionManifest = TransactionManifest(
    instructions = Instructions.fromInstructions(
        instructions = instructions().instructionsList().toMutableList().apply {
            add(
                index,
                guaranteeInstruction(
                    resourceAddress = address,
                    guaranteedAmount = guaranteedAmount
                )
            )
        }.toList(),
        networkId = instructions().networkId()
    ),
    blobs = blobs()
)

private fun guaranteeInstruction(
    resourceAddress: String,
    guaranteedAmount: BigDecimal
): Instruction {
    return Instruction.AssertWorktopContains(
        resourceAddress = Address(resourceAddress),
        amount = guaranteedAmount.toRETDecimal(roundingMode = RoundingMode.HALF_UP)
    )
}


// TODO make internal
@Suppress("MagicNumber")
fun BigDecimal.toRETDecimal(roundingMode: RoundingMode): Decimal = Decimal(setScale(18, roundingMode).toPlainString())