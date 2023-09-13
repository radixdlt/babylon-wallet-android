@file:OptIn(ExperimentalAnimationApi::class)

package com.babylon.wallet.android.presentation.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.google.accompanist.navigation.animation.composable

private const val ROUTE = "block_preview_screen"

fun NavGraphBuilder.blockPreviewAppScreen(
    onCloseApp: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE
    ) {
        BlockPreviewAppScreen(onCloseApp = onCloseApp)
    }
}

fun NavController.navigateToBlockPreviewApp() {
    navigate(ROUTE)
}

@Suppress("SwallowedException")
@Composable
fun BlockPreviewAppScreen(
    modifier: Modifier = Modifier,
    onCloseApp: () -> Unit
) {
    BackHandler(onBack = onCloseApp)
    Scaffold(
        modifier = modifier,
        contentColor = RadixTheme.colors.white,
        containerColor = RadixTheme.colors.blue1
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    ),
                text = "PREVIEW OF WALLET HAS ENDED",
                style = RadixTheme.typography.title,
                textAlign = TextAlign.Center
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingLarge),
                text = "Uninstall this app and download the Radix Wallet app from Play Store",
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(RadixTheme.dimensions.paddingLarge),
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null
                )
            }

            val context = LocalContext.current
            RadixPrimaryButton(
                modifier = Modifier
                    .padding(RadixTheme.dimensions.paddingLarge)
                    .fillMaxWidth(),
                text = "Open Play Store",
                onClick = {
                    val productionPackageName = "com.radixpublishing.radixwallet.android"
                    try {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$productionPackageName")
                            )
                        )
                    } catch (activityNotFound: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            R.string.addressAction_noWebBrowserInstalled,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun BlockPreviewAppScreenPreview() {
    RadixWalletTheme {
        BlockPreviewAppScreen(onCloseApp = {})
    }
}
