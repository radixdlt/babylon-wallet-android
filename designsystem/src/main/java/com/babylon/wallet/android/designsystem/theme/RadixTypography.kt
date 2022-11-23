package com.babylon.wallet.android.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.Typography
import com.babylon.wallet.android.designsystem.R

private val IBMPlex = FontFamily(
    Font(
        resId = R.font.ibmplex_sans_regular,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ),
    Font(
        resId = R.font.ibmplex_sans_medium,
        weight = FontWeight.Medium,
        style = FontStyle.Normal
    ),
    Font(
        resId = R.font.ibmplex_sans_semibold,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal
    ),
    Font(
        resId = R.font.ibmplex_sans_bold,
        weight = FontWeight.Bold,
        style = FontStyle.Normal
    ),
)

data class RadixTypography(
    var title: TextStyle = bold.copy(fontSize = 32.sp, lineHeight = 36.sp),
    var header: TextStyle = semibold.copy(fontSize = 20.sp, lineHeight = 23.sp),
    var secondarHeader: TextStyle = semibold.copy(fontSize = 18.sp, lineHeight = 23.sp),
    var body1Header: TextStyle = semibold.copy(fontSize = 16.sp, lineHeight = 23.sp),
    var body1HighImportance: TextStyle = medium.copy(fontSize = 16.sp, lineHeight = 23.sp),
    var body1Regular: TextStyle = regular.copy(fontSize = 16.sp, lineHeight = 23.sp),
    var body1StandaloneLink: TextStyle = semibold.copy(fontSize = 16.sp, lineHeight = 23.sp),
    var body1Link: TextStyle = medium.copy(fontSize = 16.sp, lineHeight = 23.sp),
    var body2Header: TextStyle = bold.copy(fontSize = 14.sp, lineHeight = 18.sp),
    var body2HighImportance: TextStyle = bold.copy(fontSize = 14.sp, lineHeight = 18.sp),
    var body2Regular: TextStyle = bold.copy(fontSize = 14.sp, lineHeight = 18.sp),
    var body2Link: TextStyle = bold.copy(fontSize = 14.sp, lineHeight = 18.sp),
    var button: TextStyle = bold.copy(fontSize = 14.sp, lineHeight = 18.sp),
) {
    private companion object {
        val regular = TextStyle(
            fontFamily = IBMPlex,
            fontWeight = FontWeight.Normal
        )
        val medium = TextStyle(
            fontFamily = IBMPlex,
            fontWeight = FontWeight.Medium
        )
        val semibold = TextStyle(
            fontFamily = IBMPlex,
            fontWeight = FontWeight.SemiBold
        )
        val bold = TextStyle(
            fontFamily = IBMPlex,
            fontWeight = FontWeight.Bold
        )
    }
}

// Set of Material typography styles to start with
val DefaultTypography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    h1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp
    ),
    h4 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    h6 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp
    ),
    /* Other default text styles to override
    button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
    */
)
