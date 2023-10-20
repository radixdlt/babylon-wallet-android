package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Security(
    @SerialName("isDeveloperModeEnabled")
    val isDeveloperModeEnabled: Boolean,
    @SerialName("structureConfigurationReferences")
    val structureConfigurationReferences: List<Boolean>,
    @SerialName("isCloudProfileSyncEnabled")
    val isCloudProfileSyncEnabled: Boolean
) {

    companion object {
        val default = Security(
            isDeveloperModeEnabled = false,
            // Will be fixed later: https://rdxworks.slack.com/archives/C03Q8QK1GLW/p1692805178941029
            structureConfigurationReferences = emptyList(),
            isCloudProfileSyncEnabled = true
        )
    }
}
