package com.babylon.wallet.android.presentation.transfer.assets

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.babylon.wallet.android.domain.model.Resource

@Composable
fun ChooseAssetsFungiblesContent(
    modifier: Modifier = Modifier,
    resources: List<Resource.FungibleResource>,
    selectedResources: List<Resource>,
    onResourceClicked: (Resource.FungibleResource) -> Unit
) {
    LazyColumn(
        modifier = modifier
    ) {
        
    }
}
