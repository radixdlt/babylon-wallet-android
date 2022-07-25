package com.babylon.wallet.android.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.babylon.wallet.android.R
import com.babylon.wallet.android.ui.theme.RadixGrey2

@Composable
fun WalletBalanceView(
    currencySignValue: String,
    value: String,
    hidden: Boolean,
    balanceClicked: () -> Unit
) {
    var balanceHidden by rememberSaveable { mutableStateOf(hidden) }

    ConstraintLayout(
        modifier = Modifier.fillMaxWidth(),
    ) {

        val (currencySignRef, valueContent, eyeRef) = createRefs()

        Text(
            text = currencySignValue,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(currencySignRef)
            {
                top.linkTo(parent.top, margin = 6.dp)
                bottom.linkTo(parent.bottom, margin = 6.dp)
                end.linkTo(valueContent.start)
            })

        Box(modifier = Modifier
            .constrainAs(valueContent) {
                centerVerticallyTo(parent)
                centerHorizontallyTo(parent)
            }) {
            if (balanceHidden) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) {
                        Canvas(
                            modifier = Modifier
                                .size(size = 20.dp)
                                .padding(4.dp)
                        ) {
                            drawCircle(
                                color = Color.LightGray
                            )
                        }
                    }
                }
            } else {
                Text(
                    value,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(
            onClick = {
                balanceHidden = !balanceHidden
                balanceClicked()
            },
            modifier = Modifier.constrainAs(eyeRef) {
                top.linkTo(parent.top, margin = 6.dp)
                bottom.linkTo(parent.bottom, margin = 6.dp)
                start.linkTo(valueContent.end, margin = 25.dp)
                end.linkTo(parent.end, margin = 16.dp)
            }
        ) {
            Icon(
                imageVector = if (balanceHidden)
                    ImageVector.vectorResource(id = R.drawable.ic_eye_closed)
                else
                    ImageVector.vectorResource(id = R.drawable.ic_eye_open),
                "",
                tint = RadixGrey2
            )
        }

    }
}