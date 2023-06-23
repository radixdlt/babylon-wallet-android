package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator

data class TrustedContactFactorSource(
    override val id: FactorSource.FactorSourceID.FromAddress,
    override val common: FactorSource.Common,
    @SerialName("contact")
    val contact: Contact
) : FactorSource {

    @Serializable
    data class Contact(
        @SerialName("email")
        val email: String,
        @SerialName("name")
        val name: String
    )

    companion object {

        fun newSource(
            accountAddress: FactorSource.AccountAddress,
            emailAddress: String,
            name: String,
        ): TrustedContactFactorSource {
            return TrustedContactFactorSource(
                id = FactorSource.FactorSourceID.FromAddress(
                    kind = FactorSourceKind.TRUSTED_CONTACT,
                    body = accountAddress,
                ),
                common = FactorSource.Common(
                    cryptoParameters = FactorSource.Common.CryptoParameters.babylon,
                    addedOn = InstantGenerator(),
                    lastUsedOn = InstantGenerator()
                ),
                contact = Contact(
                    email = emailAddress,
                    name = name
                )
            )
        }
    }
}
