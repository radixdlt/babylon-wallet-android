package com.babylon.wallet.android.utils.logger

import android.content.Context
import android.net.Uri
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.storage.FileRepository
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistentLogger @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val fileRepository: FileRepository
) : Timber.DebugTree() {

    private val db = LoggerDatabase.factory(applicationContext)

    init {
        scope.launch(context = dispatcher) {
            db.logger().clearEarlierLogs()
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)

        if (tag != null && excludedTags.any { it.startsWith(tag) }) {
            return
        }

        val entity = LogEntity(
            id = 0,
            timestamp = Date(),
            priority = priority,
            tag = tag,
            message = message,
            throwableTrace = t?.stackTraceToString()
        )

        scope.launch(context = dispatcher) {
            db.logger().insertLog(entity)
        }
    }

    suspend fun exportToFile(file: Uri) = withContext(dispatcher) {
        val logs = db.logger().getLogs()

        val builder = StringBuilder()

        logs.forEach {
            builder.appendLine(it.asLogEntry())
        }

        fileRepository.save(toFile = file, data = builder.toString())
    }

    companion object {
        private val excludedTags = listOf(
            "http-log",
            "AccountsStateCache"
        )
    }
}
