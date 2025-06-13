package com.babylon.wallet.android.data.repository.discover

import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.radixdlt.sargon.BlogPost
import com.radixdlt.sargon.BlogPostsClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface DiscoverRepository {

    suspend fun fetchBlogPosts(): Result<List<BlogPost>>

    fun getCachedBlogPosts(): List<BlogPost>
}

class DiscoverRepositoryImpl @Inject constructor(
    private val client: BlogPostsClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DiscoverRepository {

    private var cachedBlogPosts = emptyList<BlogPost>()

    override suspend fun fetchBlogPosts(): Result<List<BlogPost>> = withContext(ioDispatcher) {
        runCatching { client.getBlogPosts() }
            .onSuccess { blogPosts ->
                cachedBlogPosts = blogPosts
            }
    }

    override fun getCachedBlogPosts(): List<BlogPost> = cachedBlogPosts
}
