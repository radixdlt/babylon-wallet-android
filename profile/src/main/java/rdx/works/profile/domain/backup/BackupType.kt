package rdx.works.profile.domain.backup

import kotlinx.serialization.Serializable
import rdx.works.core.serializers.StringUrlEncodedSerializer

@Serializable
sealed interface BackupType {

    @Serializable
    data class Cloud(
        val entity: CloudBackupFileEntity
    ) : BackupType

    @Serializable
    data object DeprecatedCloud : BackupType

    @Serializable
    sealed interface File : BackupType {
        @Serializable
        data object PlainText : File

        @Serializable
        data class Encrypted(
            @Serializable(with = StringUrlEncodedSerializer::class)
            val password: String
        ) : File
    }
}
