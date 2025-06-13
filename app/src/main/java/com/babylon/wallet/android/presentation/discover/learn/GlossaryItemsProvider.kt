package com.babylon.wallet.android.presentation.discover.learn

import android.content.Context
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.dialogs.info.resolveTextResFromGlossaryItem
import com.babylon.wallet.android.presentation.discover.common.views.resolveDescriptionResFromGlossaryItem
import com.babylon.wallet.android.presentation.discover.common.views.resolveTitleResFromGlossaryItem
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class GlossaryItemsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val searchableContentMap = searchableGlossaryItems.associate { item ->
        item to SearchableContent.from(context, item)
    }

    fun search(query: String): List<GlossaryItem> {
        val normalizedQuery = query.trim().lowercase()

        if (normalizedQuery.isEmpty()) {
            return searchableGlossaryItems
        }

        val queryParts = normalizedQuery.splitByWhitespace()

        return searchableGlossaryItems
            .mapNotNull { item ->
                val content = searchableContentMap[item] ?: return@mapNotNull null
                val relevance = when {
                    content.title.contains(normalizedQuery) -> 0
                    content.description.contains(normalizedQuery) -> 1
                    content.text.contains(normalizedQuery) -> 2
                    content.titleParts.partiallyMatches(queryParts) -> 3
                    content.descriptionParts.partiallyMatches(queryParts) -> 4
                    content.textParts.partiallyMatches(queryParts) -> 5
                    else -> 6
                }
                item to relevance
            }
            .filter { it.second < 6 }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun List<String>.partiallyMatches(otherParts: List<String>): Boolean =
        otherParts.all { otherPart ->
            any { part -> part.contains(otherPart) }
        }

    private data class SearchableContent(
        val title: String,
        val description: String,
        val text: String,
        val titleParts: List<String>,
        val descriptionParts: List<String>,
        val textParts: List<String>
    ) {

        companion object {

            fun from(
                context: Context,
                item: GlossaryItem
            ): SearchableContent {
                val title = context.getString(item.resolveTitleResFromGlossaryItem()).lowercase()
                val description =
                    context.getString(item.resolveDescriptionResFromGlossaryItem()).lowercase()
                val text = context.getString(item.resolveTextResFromGlossaryItem()).lowercase()

                return SearchableContent(
                    title = title,
                    description = description,
                    text = text,
                    titleParts = title.splitByWhitespace(),
                    descriptionParts = description.splitByWhitespace(),
                    textParts = text.splitByWhitespace()
                )
            }
        }
    }

    companion object {

        val searchableGlossaryItems =
            GlossaryItem.entries.filterNot { it in GlossaryItem.mfaRelated }
    }
}

private fun String.splitByWhitespace(): List<String> = trim()
    .split(" ")
    .map { it.trim().lowercase() }
