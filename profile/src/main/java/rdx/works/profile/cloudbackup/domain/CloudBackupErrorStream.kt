package rdx.works.profile.cloudbackup.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.cloudbackup.model.BackupServiceException
import javax.inject.Inject

interface CloudBackupErrorStream {

    val errors: StateFlow<BackupServiceException?>

    fun onError(error: BackupServiceException)

    fun resetErrors()
}

class CloudBackupErrorStreamImpl @Inject constructor() : CloudBackupErrorStream {

    override val errors: MutableStateFlow<BackupServiceException?> = MutableStateFlow(null)

    override fun onError(error: BackupServiceException) {
        errors.update { error }
    }

    override fun resetErrors() {
        errors.update { null }
    }
}
