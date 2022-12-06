package com.babylon.wallet.android.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

data class RadixShapes(
    val circle: CornerBasedShape = CircleShape,
    val roundedRectXSmall: CornerBasedShape = RoundedCornerShape(4.dp),
    val roundedRectSmall: CornerBasedShape = RoundedCornerShape(8.dp),
    val roundedRectMedium: CornerBasedShape = RoundedCornerShape(12.dp),
    val roundedRectTopMedium: CornerBasedShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    val roundedRectBottomMedium: CornerBasedShape = RoundedCornerShape(bottomEnd = 8.dp, bottomStart = 8.dp),
    val roundedRectTopDefault: CornerBasedShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
)
