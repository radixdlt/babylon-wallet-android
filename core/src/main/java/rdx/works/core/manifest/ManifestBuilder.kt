package rdx.works.core.manifest

import com.radixdlt.ret.Instruction
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.ManifestValueKind
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.toUByteList

/**
 * A [TransactionManifest] builder.
 *
 * This class implements the builder pattern and allows building of [TransactionManifest]s through
 * builder syntax. In addition to that, this class is meant to provide an abstraction layer to the
 * creation of manifests, thus allowing for simpler interfaces and APIs for developers.
 *
 * Each [Instruction] can have one or more [ManifestBuilder] for it. As an example, this class
 * provides the [withdrawFromAccount] function which does not have an instruction for itself.
 * Instead, it calls the "`withdraw`" method through the [Instruction.CallMethod] instruction on the
 * account component to perform the withdrawal of tokens.
 *
 * Note: the current strategy for function overloading is to provide function overloads in the
 * following two cases:
 * 1. If the instruction requires a [ManifestAstValue.String], we provide an overload that makes them also
 *    accept a [kotlin.String].
 * 2. If the instruction accepts a variable number of arguments, we provide an overload that uses
 *    kotlin's `vararg` keyword.
 */
class ManifestBuilder {
    var instructions: MutableList<Instruction> = mutableListOf()
    var blobs: MutableList<ByteArray> = mutableListOf()

    /**
     * Adds a raw instruction to the builder's instruction.
     *
     * @param instruction The instruction to add to the builder instructions.
     * @return A [ManifestBuilder] with the updated state. This allows for the chaining builder
     *   pattern.
     */
    fun addInstruction(instruction: Instruction): ManifestBuilder {
        this.instructions.add(instruction)
        return this
    }

    /**
     * Adds an [Instruction.CallMethod] instruction to the set of instructions.
     *
     * With the given arguments, this method adds an [Instruction.CallMethod] instruction to the
     * manifest's vector of instructions.
     *
     * @param componentAddress The address of the component that the method exists on.
     * @param methodName The name of the method to call.
     * @param arguments An optional array of arguments to call the method with.
     * @return A [ManifestBuilder] with the updated state. This allows for the chaining builder
     */
    fun callMethod(
        componentAddress: ManifestValue.AddressValue,
        methodName: ManifestValue.StringValue,
        arguments: ManifestValue.ArrayValue? = null
    ): ManifestBuilder {
        return this.addInstruction(
            Instruction.CallMethod(
                address = componentAddress.value,
                methodName = methodName.value,
                args = arguments ?: ManifestValue.ArrayValue(
                    elementValueKind = ManifestValueKind.BOOL_VALUE,
                    elements = emptyList()
                )
            )
        )
    }

    /**
     * Builds the final [TransactionManifest].
     *
     * This is the final method in the [ManifestBuilder] which builds the [TransactionManifest].
     *
     * @return The built transaction manifest
     */
    fun build(networkId: Int): TransactionManifest {
        return TransactionManifest(
            instructions = Instructions.fromInstructions(
                instructions = instructions,
                networkId = networkId.toUByte()
            ),
            blobs = blobs.map { it.toUByteList() }
        )
    }
}
