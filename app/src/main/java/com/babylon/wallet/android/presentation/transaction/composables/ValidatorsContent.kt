package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.assets.dashedCircleBorder
import com.babylon.wallet.android.presentation.ui.composables.resources.ValidatorDetailsItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Composable
fun ValidatorsContent(
    validators: ImmutableList<ValidatorDetail>,
    modifier: Modifier = Modifier,
    text: String
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = RadixTheme.dimensions.paddingXLarge)
            .padding(bottom = RadixTheme.dimensions.paddingSmall),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Image(
                modifier = Modifier
                    .size(24.dp)
                    .dashedCircleBorder(RadixTheme.colors.gray3),
                painter = painterResource(id = DSR.ic_validator),
                contentDescription = null,
                colorFilter = ColorFilter.tint(RadixTheme.colors.gray2),
                contentScale = ContentScale.Inside
            )
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Text(
                    modifier = Modifier.weight(1f, false),
                    maxLines = 2,
                    text = text,
                    style = RadixTheme.typography.body1Link,
                    color = RadixTheme.colors.gray2,
                    overflow = TextOverflow.Ellipsis,
                )
                val iconRes = if (expanded) {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_up
                } else {
                    com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_down
                }
                Icon(
                    painter = painterResource(id = iconRes),
                    tint = RadixTheme.colors.gray2,
                    contentDescription = "arrow"
                )
            }
        }
    }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val lastItem = validators.last()

        Column(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .padding(bottom = RadixTheme.dimensions.paddingXLarge)
                .fillMaxWidth()
                .background(RadixTheme.colors.gray5)
        ) {
            validators.forEach { validator ->
                ValidatorDetailsItem(
                    iconSize = 44.dp,
                    validator = validator,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RadixTheme.colors.defaultBackground, RadixTheme.shapes.roundedRectMedium)
                        .padding(RadixTheme.dimensions.paddingDefault)
                )
                if (lastItem != validator) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StakingToValidatorsContentPreview() {
    RadixWalletTheme {
        Column {
            ValidatorsContent(
                persistentListOf(
                    ValidatorDetail("validator_tdx_19jd32jd3928jd3892jd329", BigDecimal(1000)),
                    ValidatorDetail("validator_tdx_19jd32jd3928jd3892jd329", BigDecimal(1000000))
                ),
                text = "Staking to Validators Staking to Validators Staking to Validators Staking to Validators".uppercase()
            )
        }
    }
}
