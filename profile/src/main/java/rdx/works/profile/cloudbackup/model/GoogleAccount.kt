package rdx.works.profile.cloudbackup.model

import android.net.Uri

data class GoogleAccount(
    val email: String,
    val name: String?,
    val photoUrl: Uri?
)
