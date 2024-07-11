package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.utils.formattedSpans
import kotlinx.collections.immutable.ImmutableList

@Suppress("SpreadOperator")
@Composable
fun NumberedValuesList(
    modifier: Modifier = Modifier,
    style: TextStyle = RadixTheme.typography.body2Regular,
    color: Color = RadixTheme.colors.gray1,
    values: ImmutableList<String>
) {
    val constraints = ConstraintSet {
        val idRefs = List(values.size) { index ->
            createRefFor("id$index")
        }

        val valueRefs = List(values.size) { index ->
            createRefFor("value$index")
        }

        val idsBarrier = createEndBarrier(*idRefs.toTypedArray(), margin = 4.dp)
        values.forEachIndexed { index, _ ->
            val idRef = idRefs[index]
            constrain(idRef) {
                if (index == 0) {
                    start.linkTo(parent.start, margin = 4.dp)
                    top.linkTo(parent.top)
                } else {
                    start.linkTo(idRefs[index - 1].start)
                    top.linkTo(valueRefs[index - 1].bottom, margin = 4.dp)
                }
            }

            val valueRef = valueRefs[index]
            constrain(valueRef) {
                start.linkTo(idsBarrier)
                top.linkTo(idRef.top)
                end.linkTo(parent.end)

                width = Dimension.fillToConstraints
            }
        }
    }

    ConstraintLayout(
        modifier = modifier,
        constraintSet = constraints
    ) {
        values.forEachIndexed { index, value ->
            Text(
                modifier = Modifier.layoutId("id$index"),
                text = "${index + 1}.",
                style = style,
                color = color
            )

            Text(
                modifier = Modifier.layoutId("value$index"),
                text = value.formattedSpans(
                    boldStyle = RadixTheme.typography.body2HighImportance.toSpanStyle()
                ),
                style = style,
                color = color
            )
        }
    }
}
