package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaField
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.samples.sample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.sargon.PersonaDataField

class PersonaUiModelTest {

    private val persona = Profile.sample().networks.first().personas.first()
    private val emails = persona.personaData.emailAddresses.collection.size

    private fun asking(kind: PersonaDataField.Kind, quantity: Int) = PersonaUiModel(
        persona,
        selected = true,
        requiredPersonaFields = RequiredPersonaFields(
            listOf(
                RequiredPersonaField(
                    kind,
                    DappToWalletInteraction.NumberOfValues(quantity, DappToWalletInteraction.NumberOfValues.Quantifier.Exactly)
                )
            )
        )
    )

    @Test
    fun `a persona with fewer email addresses than asked for misses them`() {
        // Shared anyway, this closed the app: the sharing required as many as were asked for.
        val model = asking(PersonaDataField.Kind.EmailAddress, emails + 1)

        assertEquals(listOf(PersonaDataField.Kind.EmailAddress), model.missingFieldKinds())
        assertFalse(model.hasRequiredData)
    }

    @Test
    fun `a persona with as many as asked for misses nothing`() {
        val model = asking(PersonaDataField.Kind.EmailAddress, emails)

        assertTrue(model.missingFieldKinds().isEmpty())
        assertTrue(model.hasRequiredData)
    }

    @Test
    fun `nothing asked for means nothing missing`() {
        assertTrue(PersonaUiModel(persona).hasRequiredData)
    }
}
