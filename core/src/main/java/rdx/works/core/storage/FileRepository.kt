package rdx.works.core.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.buffer
import okio.source
import javax.inject.Inject

interface FileRepository {
    fun save(toFile: Uri, data: String): Result<Unit>
    fun read(fromFile: Uri): Result<String>
}

class FileRepositoryImpl @Inject constructor(
    @ApplicationContext val context: Context
) : FileRepository {

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

    override fun read(fromFile: Uri): Result<String> {
        return try {
            val fileContent = context.contentResolver.openInputStream(fromFile)?.use { stream ->
                stream.source().buffer().readUtf8()
            } ?: return Result.failure(NullPointerException("File not found"))

            Result.success(fileContent)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
