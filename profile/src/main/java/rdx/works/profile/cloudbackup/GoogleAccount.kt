package rdx.works.profile.cloudbackup

import android.net.Uri

data class GoogleAccount(
    val email: String,
    val name: String?,
    val photoUrl: Uri?
)
