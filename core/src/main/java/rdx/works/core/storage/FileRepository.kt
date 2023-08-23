package rdx.works.core.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface FileRepository {

    fun save(toFile: Uri, data: String): Result<Unit>

}

class FileRepositoryImpl @Inject constructor(
    @ApplicationContext val context: Context
): FileRepository {

    override fun save(toFile: Uri, data: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(toFile)?.use { stream ->
                stream.bufferedWriter().use { it.write(data) }
            }

            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

}
