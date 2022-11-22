package com.babylon.wallet.android.presentation.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.ui.unit.dp

data class RadixShapes(
    val circle: CornerBasedShape = CircleShape,
    val roundedRectXSmall: CornerBasedShape = RoundedCornerShape(4.dp),
    val roundedRectSmall: CornerBasedShape = RoundedCornerShape(8.dp)
)

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)
