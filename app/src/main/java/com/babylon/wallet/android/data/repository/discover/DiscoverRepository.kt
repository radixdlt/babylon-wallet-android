package com.babylon.wallet.android.data.repository.discover

import android.content.Context
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.radixdlt.sargon.BlogPost
import com.radixdlt.sargon.BlogPostsClient
import com.radixdlt.sargon.os.driver.AndroidFileSystemDriver
import com.radixdlt.sargon.os.driver.AndroidNetworkingDriver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import rdx.works.core.di.GatewayHttpClient
import javax.inject.Inject

interface DiscoverRepository {

    suspend fun getBlogPosts(): Result<List<BlogPost>>

    suspend fun getNewBlogPost(): Result<BlogPost?>
}

class DiscoverRepositoryImpl @Inject constructor(
    @GatewayHttpClient private val httpClient: OkHttpClient,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DiscoverRepository {

    private val blogPostsClient by lazy {
        BlogPostsClient(
            networkingDriver = AndroidNetworkingDriver(httpClient),
            fileSystemDriver = AndroidFileSystemDriver(context)
        )
    }

    override suspend fun getBlogPosts(): Result<List<BlogPost>> = withContext(ioDispatcher) {
        runCatching { blogPostsClient.getBlogPosts() }
            .map { it.posts }
    }

    override suspend fun getNewBlogPost(): Result<BlogPost?> = withContext(ioDispatcher) {
        runCatching { blogPostsClient.getBlogPosts() }
            .map { it.newBlogPost }
    }
}
