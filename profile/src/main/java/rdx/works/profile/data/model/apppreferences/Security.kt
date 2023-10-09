@file:OptIn(ExperimentalSerializationApi::class)

package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import java.time.Instant

@Serializable
data class Security(
    @SerialName("isDeveloperModeEnabled")
    val isDeveloperModeEnabled: Boolean,
    @SerialName("structureConfigurationReferences")
    val structureConfigurationReferences: List<StructureConfigurationReference>,
    @SerialName("isCloudProfileSyncEnabled")
    val isCloudProfileSyncEnabled: Boolean
) {

    @Serializable
    data class StructureConfigurationReference(
        val metadata: Metadata,
        val configuration: Configuration
    ) {

        @Serializable
        data class Metadata(
            @EncodeDefault
            val id: String? = null,
            @EncodeDefault
            val label: String = "",
            @Contextual
            @EncodeDefault
            val createdOn: Instant? = null,
            @Contextual
            @EncodeDefault
            val lastUpdatedOn: Instant? = null
        )

        @Serializable
        data class Configuration(
            val numberOfDaysUntilAutoConfirmation: Int, // ?
            val primaryRole: Map<Role.PrimaryRoleTag, FactorSource.FactorSourceID>,
            val recoveryRole: Map<Role.RecoveryRoleTag, FactorSource.FactorSourceID>,
            val confirmationRole: Map<Role.ConfirmationRoleTag, FactorSource.FactorSourceID>
        ) {

            @Serializable
            sealed interface Role {

                enum class SecurityStructureRole {
                    Primary, Recovery, Confirmation
                }

                val role: SecurityStructureRole

                @Serializable
                data object PrimaryRoleTag : Role {
                    override val role: SecurityStructureRole = SecurityStructureRole.Primary
                }

                @Serializable
                data object RecoveryRoleTag : Role {
                    override val role: SecurityStructureRole = SecurityStructureRole.Recovery
                }

                @Serializable
                data object ConfirmationRoleTag : Role {
                    override val role: SecurityStructureRole = SecurityStructureRole.Confirmation
                }
            }
        }
    }

    companion object {
        val default = Security(
            isDeveloperModeEnabled = false,
            // Will be fixed later: https://rdxworks.slack.com/archives/C03Q8QK1GLW/p1692805178941029
            structureConfigurationReferences = emptyList(),
            isCloudProfileSyncEnabled = true
        )
    }
}

fun Profile.updateDeveloperMode(isEnabled: Boolean): Profile = copy(
    appPreferences = appPreferences.copy(
        security = appPreferences.security.copy(
            isDeveloperModeEnabled = isEnabled
        )
    )
)

fun Profile.updateCloudSyncEnabled(isEnabled: Boolean) = copy(
    appPreferences = appPreferences.copy(
        security = appPreferences.security.copy(
            isCloudProfileSyncEnabled = isEnabled
        )
    )
)

fun Security.StructureConfigurationReference.Configuration.isSimple(): Boolean {
    val hasSingleFactorSourceOfDevice = this.primaryRole.values.map {
        it.kind
    }.contains(FactorSourceKind.DEVICE)

    val hasSingleFactorSourceOfTrustedContact = this.recoveryRole.values.map {
        it.kind
    }.contains(FactorSourceKind.TRUSTED_CONTACT)

    val hasSingleFactorSourceOfSecurityQuestions = this.confirmationRole.values.map {
        it.kind
    }.contains(FactorSourceKind.SECURITY_QUESTIONS)

    return hasSingleFactorSourceOfDevice && hasSingleFactorSourceOfTrustedContact && hasSingleFactorSourceOfSecurityQuestions
}
