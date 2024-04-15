package com.babylon.wallet.android.presentation.googlesignin

import android.net.Uri

data class GoogleAccount(
    val email: String,
    val name: String?,
    val photoUrl: Uri?
)
