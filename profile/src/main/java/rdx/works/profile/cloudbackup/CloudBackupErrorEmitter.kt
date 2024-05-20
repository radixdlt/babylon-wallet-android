package rdx.works.profile.cloudbackup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
