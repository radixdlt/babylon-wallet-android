package com.babylon.wallet.android.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

data class RadixShapes(
    val circle: CornerBasedShape = CircleShape,
    val roundedRectXSmall: CornerBasedShape = RoundedCornerShape(4.dp),
    val roundedRectSmall: CornerBasedShape = RoundedCornerShape(8.dp),
    val roundedRectMedium: CornerBasedShape = RoundedCornerShape(12.dp)
)