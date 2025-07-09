package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.common

import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.EntitiesLinkedToFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.id
import kotlinx.collections.immutable.toPersistentList
import kotlin.collections.orEmpty
import kotlin.collections.plus

fun List<FactorSource>.toUiItems(
    entitiesLinkedToFactorSourceById: Map<FactorSourceId, EntitiesLinkedToFactorSource>,
    statusMessagesByFactorSourceId: Map<FactorSourceId, List<FactorSourceStatusMessage>>,
    alreadySelectedFactorSources: List<FactorSourceId> = emptyList(),
    unusableFactorSources: List<FactorSourceId> = emptyList(),
): List<Selectable<FactorSourceCard>> = map { factorSource ->
    factorSource.toUiItem(
        entitiesLinkedToFactorSourceById = entitiesLinkedToFactorSourceById,
        statusMessagesByFactorSourceId = statusMessagesByFactorSourceId,
        alreadySelectedFactorSources = alreadySelectedFactorSources,
        unusableFactorSources = unusableFactorSources
    )
}

fun FactorSource.toUiItem(
    entitiesLinkedToFactorSourceById: Map<FactorSourceId, EntitiesLinkedToFactorSource>,
    statusMessagesByFactorSourceId: Map<FactorSourceId, List<FactorSourceStatusMessage>>,
    alreadySelectedFactorSources: List<FactorSourceId> = emptyList(),
    unusableFactorSources: List<FactorSourceId> = emptyList(),
): Selectable<FactorSourceCard> {
    val messages = statusMessagesByFactorSourceId.getOrDefault(id, emptyList())
        // We don't want to show the success checkmark indicating the factor source was backed up
        .filterNot { it is FactorSourceStatusMessage.NoSecurityIssues }
    val linkedEntities = entitiesLinkedToFactorSourceById[id]

    val cannotBeUsedHereMessage = if (id in unusableFactorSources) {
        listOf(FactorSourceStatusMessage.CannotBeUsedHere)
    } else {
        emptyList()
    }
    val isFactorSourceLost = messages.contains(FactorSourceStatusMessage.SecurityPrompt.LostFactorSource)
    val isSelected = id in alreadySelectedFactorSources

    return Selectable(
        toFactorSourceCard(
            isEnabled = !isSelected && !isFactorSourceLost && id !in unusableFactorSources,
            messages = (messages + cannotBeUsedHereMessage).toPersistentList(),
            accounts = linkedEntities?.accounts.orEmpty().toPersistentList(),
            personas = linkedEntities?.personas.orEmpty().toPersistentList(),
            hasHiddenEntities = !linkedEntities?.hiddenAccounts.isNullOrEmpty() ||
                !linkedEntities?.hiddenPersonas.isNullOrEmpty()
        ),
        selected = isSelected
    )
}
