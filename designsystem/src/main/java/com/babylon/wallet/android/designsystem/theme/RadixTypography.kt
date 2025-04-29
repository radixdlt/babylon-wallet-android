package com.babylon.wallet.android.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    var title: TextStyle = RadixTypography.title,
    var header: TextStyle = RadixTypography.header,
    var secondaryHeader: TextStyle = RadixTypography.secondaryHeader,
    var body1Header: TextStyle = RadixTypography.body1Header,
    var body1HighImportance: TextStyle = RadixTypography.body1HighImportance,
    var body1Regular: TextStyle = RadixTypography.body1Regular,
    var body1StandaloneLink: TextStyle = RadixTypography.body1StandaloneLink,
    var body1Link: TextStyle = RadixTypography.body1Link,
    var body2Header: TextStyle = RadixTypography.body2Header,
    var body2HighImportance: TextStyle = RadixTypography.body2HighImportance,
    var body2Regular: TextStyle = RadixTypography.body2Regular,
    var body3Regular: TextStyle = RadixTypography.body3Regular,
    var body2Link: TextStyle = RadixTypography.body2Link,
    var button: TextStyle = RadixTypography.button,
) {
    companion object {
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

        internal val title: TextStyle = bold.copy(fontSize = 32.sp, lineHeight = 36.sp)
        internal val header: TextStyle = semibold.copy(fontSize = 20.sp, lineHeight = 23.sp)
        internal val secondaryHeader: TextStyle = semibold.copy(fontSize = 18.sp, lineHeight = 23.sp)
        internal val body1Header: TextStyle = semibold.copy(fontSize = 16.sp, lineHeight = 23.sp)
        internal val body1HighImportance: TextStyle = medium.copy(fontSize = 16.sp, lineHeight = 23.sp)
        internal val body1Regular: TextStyle = regular.copy(fontSize = 16.sp, lineHeight = 23.sp)
        internal val body1StandaloneLink: TextStyle = semibold.copy(fontSize = 16.sp, lineHeight = 23.sp)
        internal val body1Link: TextStyle = medium.copy(fontSize = 16.sp, lineHeight = 23.sp)
        internal val body2Header: TextStyle = bold.copy(fontSize = 14.sp, lineHeight = 18.sp)
        internal val body2HighImportance: TextStyle = medium.copy(fontSize = 14.sp, lineHeight = 18.sp)
        internal val body2Regular: TextStyle = regular.copy(fontSize = 14.sp, lineHeight = 18.sp)
        internal val body3Regular: TextStyle = regular.copy(fontSize = 12.sp, lineHeight = 23.sp)
        internal val body2Link: TextStyle = medium.copy(fontSize = 14.sp, lineHeight = 18.sp)
        internal val button: TextStyle = bold.copy(fontSize = 16.sp, lineHeight = 18.sp)
    }
}

internal val RadixMaterialTypography = Typography().let { default ->
    default.copy(
        displayLarge = default.displayLarge.copy(
            fontFamily = IBMPlex
        ),
        displayMedium = default.displayMedium.copy(
            fontFamily = IBMPlex,
        ),
        displaySmall = default.displaySmall.copy(
            fontFamily = IBMPlex
        ),
        headlineLarge = default.headlineLarge.copy(
            fontFamily = IBMPlex
        ),
        headlineMedium = default.headlineMedium.copy(
            fontFamily = IBMPlex
        ),
        headlineSmall = default.headlineSmall.copy(
            fontFamily = IBMPlex
        ),
        titleLarge = default.titleLarge.copy(
            fontFamily = IBMPlex
        ),
        titleMedium = default.titleMedium.copy(
            fontFamily = IBMPlex
        ),
        titleSmall = default.titleSmall.copy(
            fontFamily = IBMPlex
        ),
        bodyLarge = default.bodyLarge.copy(
            fontFamily = IBMPlex
        ),
        bodyMedium = default.bodyMedium.copy(
            fontFamily = IBMPlex
        ),
        bodySmall = default.bodySmall.copy(
            fontFamily = IBMPlex
        ),
        labelLarge = default.labelLarge.copy(
            fontFamily = IBMPlex
        ),
        labelMedium = default.labelMedium.copy(
            fontFamily = IBMPlex
        ),
        labelSmall = default.labelSmall.copy(
            fontFamily = IBMPlex
        )
    )
}
