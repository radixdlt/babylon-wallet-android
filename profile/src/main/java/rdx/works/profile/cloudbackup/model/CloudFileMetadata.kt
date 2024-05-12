package rdx.works.profile.cloudbackup.model

import com.radixdlt.sargon.ProfileId
import com.radixdlt.sargon.Timestamp
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CloudFileMetadata(
    val profileId: ProfileId,
    private val lastUsedOnDeviceName: String,
    private val lastUsedOnDeviceModified: Timestamp,
    private val totalNumberOfAccountsOnAllNetworks: Int,
    private val totalNumberOfPersonasOnAllNetworks: Int,
) {

    fun toMap() = mapOf(
        LAST_USED_ON_DEVICE_KEY to lastUsedOnDeviceName,
        LAST_MODIFIED_KEY to lastUsedOnDeviceModified.toDateString(),
        TOTAL_NUMBER_OF_ACCOUNTS_KEY to totalNumberOfAccountsOnAllNetworks.toString(),
        TOTAL_NUMBER_OF_PERSONAS_KEY to totalNumberOfPersonasOnAllNetworks.toString()
    )

    companion object {
        const val LAST_USED_ON_DEVICE_KEY = "key_last_used_on_device"
        const val LAST_MODIFIED_KEY = "key_last_modified"
        const val TOTAL_NUMBER_OF_ACCOUNTS_KEY = "key_total_number_of_accounts"
        const val TOTAL_NUMBER_OF_PERSONAS_KEY = "key_total_number_of_personas"
    }
}

private const val LAST_USED_DATE_FORMAT_SHORT_MONTH = "d MMM yyyy"
private fun Timestamp.toDateString(): String {
    val formatter = DateTimeFormatter.ofPattern(LAST_USED_DATE_FORMAT_SHORT_MONTH).withZone(ZoneId.systemDefault())
    return formatter.format(this)
}
