package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.Profile

@Serializable
data class Security(
    @SerialName("isDeveloperModeEnabled")
    val isDeveloperModeEnabled: Boolean
) {

    companion object {
        val default = Security(isDeveloperModeEnabled = true)
    }
}

fun Profile.updateDeveloperMode(isEnabled: Boolean): Profile = copy(
    appPreferences = appPreferences.copy(
        security = appPreferences.security.copy(
            isDeveloperModeEnabled = isEnabled
        )
    )
)
