package rdx.works.profile.data.model.extensions

import rdx.works.profile.data.model.Profile

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
