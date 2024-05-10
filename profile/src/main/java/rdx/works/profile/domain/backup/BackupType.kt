package rdx.works.profile.domain.backup

import kotlinx.serialization.Serializable

@Serializable
sealed interface BackupType {
    @Serializable
    data object Cloud : BackupType

    @Serializable
    sealed interface File : BackupType {
        @Serializable
        data object PlainText : File

        @Serializable
        data class Encrypted(val password: String) : File
    }
}
