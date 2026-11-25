package com.babylon.wallet.android.presentation.transaction.model

import com.radixdlt.sargon.AccessControllerAddress
import com.radixdlt.sargon.AccessControllerPrimaryRoleLockOperation
import com.radixdlt.sargon.AccessControllerRecoveryCancellation
import com.radixdlt.sargon.AccessControllerRecoveryConfirmation
import com.radixdlt.sargon.AccessControllerRecoveryProposal
import com.radixdlt.sargon.AccessControllerRuleSetForDisplay
import com.radixdlt.sargon.AccessControllerTimedRecoveryProposal
import com.radixdlt.sargon.AccessControllerXrdTransfer
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.CreatedSecurifiedEntity
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.PrimaryRoleLockOperation
import com.radixdlt.sargon.RecoveryConfirmationKind
import com.radixdlt.sargon.RecoveryProposer

/**
 * An access controller operation on one access controller that belongs to none of the user's entities, e.g. a
 * shared account, as the review shows it: one heading for all of them, the account, one row per effect, and the
 * rules, when it sets or stops a change of them, in a section of their own.
 */
data class ControlOperation(
    val heading: Heading = Heading.UpdateAccountControl,
    /** The access controller operated on, `null` for one the manifest creates. */
    val accessController: AccessControllerAddress?,
    val controlledEntity: AddressOfAccountOrPersona?,
    /** Whether the manifest creates the entity, whose address is then only the preview's, so it isn't shown. */
    val createsEntity: Boolean = false,
    val effects: List<Effect> = emptyList(),
    val rules: AccessControllerRuleSetForDisplay? = null,
    val rulesHeading: RulesHeading = RulesHeading.New,
    /** The name the address book gives the controlled account, if any. */
    val accountName: String? = null,
    /** The controlled account, when it is one of the user's, e.g. a shared account in the Profile. */
    val ownedAccount: Account? = null,
    /** The names the address book gives the keys in the rules, e.g. other members' signature resources. */
    val keyNames: Map<NonFungibleGlobalId, String> = emptyMap()
) {

    enum class Heading {
        UpdateAccountControl,
        RecoveryFees,
        CreateAccount,
        CreatePersona
    }

    /** What the rules are to this operation, as the heading of their section says. */
    enum class RulesHeading {
        New,
        Pending,
        Cancelled,
        Rules
    }

    /** One thing the operation does, shown as an icon and a short line. */
    sealed interface Effect {
        /** Proposed and confirmed together. */
        data object Applied : Effect

        /**
         * A timed update; the engine keeps the access controller's current delay, so that is the one shown, `null` if
         * it couldn't be read. Never the delay the proposal names, which the engine doesn't apply.
         */
        data class Timed(val delayInMinutes: UInt?) : Effect

        /**
         * The access controller's current timed recovery delay, which it keeps under the new rules, `null` if it has
         * none or it couldn't be read.
         */
        data class CurrentTimedRecoveryDelay(val delayInMinutes: UInt?) : Effect

        /** The role that proposed the update. */
        data class ProposedBy(val proposer: RecoveryProposer) : Effect

        data object LocksPrimaryRole : Effect

        data object ConfirmsPendingUpdate : Effect

        data object CompletesTimedUpdate : Effect

        /** Stops the recovery role's timed update; unless it is also cancelled, it can still be confirmed. */
        data class StopsTimer(val canStillBeConfirmed: Boolean) : Effect

        /** Cancels the pending update the role proposed. */
        data class CancelsPendingUpdate(val proposer: RecoveryProposer) : Effect

        data object UnlocksPrimaryRole : Effect

        /** XRD added to the access controller's recovery fee vault. */
        data class AddsToRecoveryFee(val xrdAmount: Decimal192) : Effect

        /** XRD withdrawn from the access controller's recovery fee vault. */
        data class WithdrawsFromRecoveryFee(val xrdAmount: Decimal192) : Effect

        /** A new entity, controlled by the rules shown. */
        data object Created : Effect

        /** The timed recovery delay of an access controller the manifest creates. */
        data class TimedRecoveryDelay(val delayInMinutes: UInt) : Effect
    }

    companion object {

        /** The delays shown are always the access controller's current one: the engine ignores those a manifest names. */
        fun recovery(proposal: AccessControllerRecoveryProposal) = ControlOperation(
            accessController = proposal.accessController,
            controlledEntity = proposal.controlledEntity,
            effects = listOf(
                Effect.Applied,
                Effect.ProposedBy(proposal.proposer),
                Effect.CurrentTimedRecoveryDelay(proposal.currentTimedRecoveryDelayInMinutes)
            ),
            rules = proposal.proposedRules
        )

        fun timedRecovery(recovery: AccessControllerTimedRecoveryProposal) = ControlOperation(
            accessController = recovery.accessController,
            controlledEntity = recovery.controlledEntity,
            effects = buildList {
                add(Effect.Timed(recovery.currentTimedRecoveryDelayInMinutes))
                if (recovery.locksPrimaryRole) add(Effect.LocksPrimaryRole)
            },
            rules = recovery.proposedRules
        )

        fun confirmation(confirmation: AccessControllerRecoveryConfirmation) = ControlOperation(
            accessController = confirmation.accessController,
            controlledEntity = confirmation.controlledEntity,
            effects = listOf(
                when (confirmation.kind) {
                    RecoveryConfirmationKind.QUICK -> Effect.ConfirmsPendingUpdate
                    RecoveryConfirmationKind.TIMED -> Effect.CompletesTimedUpdate
                },
                Effect.ProposedBy(confirmation.proposer),
                Effect.CurrentTimedRecoveryDelay(confirmation.currentTimedRecoveryDelayInMinutes)
            ),
            rules = confirmation.proposedRules
        )

        /**
         * One effect per thing done: the recovery role's timer stopped, each role's proposal cancelled, the primary
         * role unlocked. The rules shown are those of the recovery role's proposal whose timer is stopped, so they are
         * cancelled only when that proposal is cancelled too.
         */
        fun cancellation(cancellation: AccessControllerRecoveryCancellation): ControlOperation {
            val cancelsRecoveryProposal = RecoveryProposer.RECOVERY in cancellation.cancelledProposers
            return ControlOperation(
                accessController = cancellation.accessController,
                controlledEntity = cancellation.controlledEntity,
                effects = buildList {
                    if (cancellation.stoppedTimedRecovery != null) {
                        add(Effect.StopsTimer(canStillBeConfirmed = !cancelsRecoveryProposal))
                    }
                    cancellation.cancelledProposers.distinct().forEach { proposer ->
                        add(Effect.CancelsPendingUpdate(proposer))
                    }
                    if (cancellation.unlocksPrimaryRole) add(Effect.UnlocksPrimaryRole)
                },
                rules = cancellation.stoppedTimedRecovery,
                rulesHeading = if (cancelsRecoveryProposal) RulesHeading.Cancelled else RulesHeading.Pending
            )
        }

        fun primaryRoleLock(operation: AccessControllerPrimaryRoleLockOperation) = ControlOperation(
            accessController = operation.accessController,
            controlledEntity = operation.controlledEntity,
            effects = listOf(
                when (operation.operation) {
                    PrimaryRoleLockOperation.LOCK -> Effect.LocksPrimaryRole
                    PrimaryRoleLockOperation.UNLOCK -> Effect.UnlocksPrimaryRole
                }
            )
        )

        /** An account or persona created with its owner badge in a new access controller, e.g. a shared account. */
        fun created(created: CreatedSecurifiedEntity) = ControlOperation(
            heading = if (created.entity is AddressOfAccountOrPersona.Account) Heading.CreateAccount else Heading.CreatePersona,
            accessController = null,
            controlledEntity = created.entity,
            createsEntity = true,
            effects = buildList {
                add(Effect.Created)
                created.timedRecoveryDelayInMinutes?.let { add(Effect.TimedRecoveryDelay(it)) }
            },
            rules = created.rules,
            rulesHeading = RulesHeading.Rules
        )

        /** XRD added to an access controller's recovery fee vault. */
        fun recoveryFeeContribution(transfer: AccessControllerXrdTransfer) = ControlOperation(
            heading = Heading.RecoveryFees,
            accessController = transfer.accessController,
            controlledEntity = transfer.controlledEntity,
            effects = listOf(Effect.AddsToRecoveryFee(transfer.xrdAmount))
        )

        /** XRD withdrawn from an access controller's recovery fee vault. */
        fun recoveryFeeWithdrawal(transfer: AccessControllerXrdTransfer) = ControlOperation(
            heading = Heading.RecoveryFees,
            accessController = transfer.accessController,
            controlledEntity = transfer.controlledEntity,
            effects = listOf(Effect.WithdrawsFromRecoveryFee(transfer.xrdAmount))
        )
    }
}
