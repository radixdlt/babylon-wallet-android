package rdx.works.profile.domain.backup

data class CloudBackupFile(
    val fileEntity: CloudBackupFileEntity,
    val serializedProfile: String
)
