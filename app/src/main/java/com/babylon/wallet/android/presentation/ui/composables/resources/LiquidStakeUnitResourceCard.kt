@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakeResources
import com.babylon.wallet.android.domain.model.assets.ValidatorsWithStakeResources
import java.math.BigDecimal

@Composable
fun LiquidStakeUnitResourceHeader(
    modifier: Modifier = Modifier,
    collection: ValidatorsWithStakeResources,
    cardHeight: Dp = 103.dp,
    collapsed: Boolean = true,
    groupInnerPadding: Dp = 6.dp,
    parentSectionClick: () -> Unit,
) {
    val bottomCorners = if (collapsed) 12.dp else 0.dp
    val cardShape = RoundedCornerShape(12.dp, 12.dp, bottomCorners, bottomCorners)
    BoxWithConstraints(
        modifier = modifier
    ) {
        if (collapsed) {
            if (collection.validators.isNotEmpty()) {
                val scaleFactor = 0.9f
                val topOffset = cardHeight * (1 - scaleFactor) + groupInnerPadding
                Surface(
                    modifier = Modifier
                        .padding(top = topOffset)
                        .fillMaxWidth()
                        .height(cardHeight)
                        .scale(scaleFactor, scaleFactor),
                    shape = RadixTheme.shapes.roundedRectMedium,
                    color = Color.White,
                    elevation = 3.dp,
                    content = {}
                )
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(cardHeight)
                .clickable { parentSectionClick() },
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = RadixTheme.colors.defaultBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            onClick = parentSectionClick
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_splash),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RadixTheme.shapes.roundedRectSmall),
                    tint = Color.Unspecified
                )
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        stringResource(id = R.string.account_poolUnits_lsuResourceHeader),
                        style = RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.gray1,
                        maxLines = 2
                    )
                    Text(
                        stringResource(id = R.string.account_poolUnits_numberOfStakes, collection.validators.size),
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.gray2,
                        maxLines = 1
                    )
                }
            }
//            Row(
//                Modifier
//                    .fillMaxWidth()
//                    .background(
//                        RadixTheme.colors.gray5,
//                        shape = if (collapsed) {
//                            RadixTheme.shapes.roundedRectBottomMedium
//                        } else {
//                            RoundedCornerShape(0.dp)
//                        }
//                    )
//                    .padding(horizontal = RadixTheme.dimensions.paddingMedium, vertical = RadixTheme.dimensions.paddingSmall)
//            ) {
//                Tag(
//                    modifier = Modifier
//                        .padding(RadixTheme.dimensions.paddingXSmall)
//                        .border(
//                            width = 1.dp,
//                            color = RadixTheme.colors.gray4,
//                            shape = RadixTheme.shapes.roundedTag
//                        )
//                        .padding(RadixTheme.dimensions.paddingSmall),
//                    tag = Resource.Tag.Official
//                )
//            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LiquidStakeUnitResourceHeaderPreview() {
    RadixWalletTheme {
        LiquidStakeUnitResourceHeader(
            modifier = Modifier.padding(all = 30.dp),
            collapsed = true,
            collection = ValidatorsWithStakeResources(
                listOf(
                    ValidatorWithStakeResources(ValidatorDetail("address1", "Validator 1", null, null, BigDecimal(100000))),
                    ValidatorWithStakeResources(ValidatorDetail("address1", "Validator 1", null, null, BigDecimal(100000)))
                )
            ),
            parentSectionClick = {}
        )
    }
}
