package rdx.works.core.domain.cloudbackup

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class GoogleDriveFileId(
    val id: String
)
