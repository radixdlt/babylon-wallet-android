package rdx.works.profile.domain.backup

import kotlinx.serialization.Serializable

@Serializable
sealed interface BackupType {
    @Serializable
    object Cloud : BackupType

    @Serializable
    sealed interface File : BackupType {
        @Serializable
        object PlainText : File

        @Serializable
        data class Encrypted(val password: String) : File
    }
}
