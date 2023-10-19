package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator
import java.time.Instant

@Serializable
@SerialName("trustedContact")
data class TrustedContactFactorSource(
    override val id: FactorSourceID.FromAddress,
    override val common: Common,
    @SerialName("contact")
    val contact: Contact
) : FactorSource() {

    @Serializable
    data class Contact(
        @SerialName("email")
        val email: String,
        @SerialName("name")
        val name: String
    )

    companion object {

        fun newSource(
            accountAddress: AccountAddress,
            emailAddress: String,
            name: String,
            createdAt: Instant = InstantGenerator()
        ): TrustedContactFactorSource {
            return TrustedContactFactorSource(
                id = FactorSourceID.FromAddress(
                    kind = FactorSourceKind.TRUSTED_CONTACT,
                    body = accountAddress,
                ),
                common = Common(
                    cryptoParameters = Common.CryptoParameters.babylon,
                    addedOn = createdAt,
                    lastUsedOn = createdAt
                ),
                contact = Contact(
                    email = emailAddress,
                    name = name
                )
            )
        }
    }

    override val identifier: String
        get() = id.body.value
}
