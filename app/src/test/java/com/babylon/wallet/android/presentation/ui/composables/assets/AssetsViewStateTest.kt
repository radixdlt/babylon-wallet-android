package com.babylon.wallet.android.presentation.ui.composables.assets

import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetsViewStateTest {

    @Test
    fun `NFT state starts in full mode with no opened collection`() {
        val state = AssetsViewState.init(selectedTab = AssetsTab.Nfts)

        assertEquals(NFTsViewMode.Full, state.nftsViewMode)
        assertNull(state.openedNFTCollectionId)
    }

    @Test
    fun `clicking an NFT collection opens and closes it`() {
        val initialState = AssetsViewState.init(selectedTab = AssetsTab.Nfts)

        val openedState = initialState.onCollectionToggle(FIRST_COLLECTION_ID)
        val closedState = openedState.onCollectionToggle(FIRST_COLLECTION_ID)

        assertEquals(FIRST_COLLECTION_ID, openedState.openedNFTCollectionId)
        assertNull(closedState.openedNFTCollectionId)
    }

    @Test
    fun `clicking another NFT collection replaces the opened collection`() {
        val initialState = AssetsViewState.init(selectedTab = AssetsTab.Nfts)

        val state = initialState
            .onCollectionToggle(FIRST_COLLECTION_ID)
            .onCollectionToggle(SECOND_COLLECTION_ID)

        assertEquals(SECOND_COLLECTION_ID, state.openedNFTCollectionId)
    }

    @Test
    fun `non NFT collections retain accordion behavior`() {
        val initialState = AssetsViewState.init(selectedTab = AssetsTab.Staking)

        val expandedState = initialState.onCollectionToggle(FIRST_COLLECTION_ID)
        val collapsedState = expandedState.onCollectionToggle(FIRST_COLLECTION_ID)

        assertTrue(initialState.isCollapsed(FIRST_COLLECTION_ID))
        assertFalse(expandedState.isCollapsed(FIRST_COLLECTION_ID))
        assertTrue(collapsedState.isCollapsed(FIRST_COLLECTION_ID))
        assertNull(expandedState.openedNFTCollectionId)
    }

    @Test
    fun `NFT view mode cycles through all modes`() {
        assertEquals(NFTsViewMode.Grid, NFTsViewMode.Full.next())
        assertEquals(NFTsViewMode.Row, NFTsViewMode.Grid.next())
        assertEquals(NFTsViewMode.Full, NFTsViewMode.Row.next())
    }

    private companion object {
        const val FIRST_COLLECTION_ID = "collection-1"
        const val SECOND_COLLECTION_ID = "collection-2"
    }
}
