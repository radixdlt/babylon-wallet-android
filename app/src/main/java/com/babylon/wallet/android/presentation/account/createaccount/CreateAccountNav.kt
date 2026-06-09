package com.babylon.wallet.android.presentation.account.createaccount

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.core.os.BundleCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.ARG_REQUEST_SOURCE
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.presentation.onboarding.eula.ROUTE_EULA_SCREEN
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.discriminant
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

private const val ARG_GATEWAY_TO_SWITCH = "arg_gateway_to_switch"

const val ROUTE_CREATE_ACCOUNT = "create_account_route" +
    "?$ARG_REQUEST_SOURCE={$ARG_REQUEST_SOURCE}" +
    "&$ARG_GATEWAY_TO_SWITCH={$ARG_GATEWAY_TO_SWITCH}"

internal class CreateAccountNavArgs(
    val requestSource: CreateAccountRequestSource?,
    val gatewayToSwitch: Gateway?
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<CreateAccountRequestSource>(ARG_REQUEST_SOURCE),
        savedStateHandle.get<GatewayToSwitchParam>(ARG_GATEWAY_TO_SWITCH)?.toGateway(),
    )
}

fun NavController.createAccountScreen(
    requestSource: CreateAccountRequestSource = CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled,
    gatewayToSwitch: Gateway? = null,
    popToRoute: String? = null
) {
    val gatewayArg = gatewayToSwitch?.let {
        Uri.encode(Serializer.kotlinxSerializationJson.encodeToString(GatewayToSwitchParam.from(it)))
    }
    val route = buildString {
        append("create_account_route")
        append("?$ARG_REQUEST_SOURCE=$requestSource")
        gatewayArg?.let {
            append("&$ARG_GATEWAY_TO_SWITCH=$it")
        }
    }

    navigate(route = route) {
        if (requestSource == CreateAccountRequestSource.FirstTimeWithCloudBackupEnabled) {
            // at this point wallet navigated from ConnectCloudBackupScreen and
            // user has authenticated/authorized access to Drive therefore
            // do not navigate back to ConnectCloudBackupScreen but to EulaScreen
            popUpTo(route = ROUTE_EULA_SCREEN) {
                inclusive = false
            }
        }
        popToRoute?.let { route ->
            popUpTo(route) {
                inclusive = true
            }
        }
    }
}

fun NavGraphBuilder.createAccountScreen(
    onBackClick: () -> Unit,
    onContinueClick: (accountId: AccountAddress, requestSource: CreateAccountRequestSource?) -> Unit
) {
    markAsHighPriority(route = ROUTE_CREATE_ACCOUNT)
    composable(
        route = ROUTE_CREATE_ACCOUNT,
        arguments = listOf(
            navArgument(ARG_REQUEST_SOURCE) {
                type = NavType.EnumType(CreateAccountRequestSource::class.java)
                defaultValue = CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled
            },
            navArgument(ARG_GATEWAY_TO_SWITCH) {
                type = OptionalGatewayParamType()
                nullable = true
            }
        ),
        enterTransition = {
            if (requiresHorizontalTransition(targetState.arguments)) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
            }
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            if (requiresHorizontalTransition(initialState.arguments)) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
            }
        }
    ) {
        CreateAccountScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}

@Keep
@Serializable
@Parcelize
private data class GatewayToSwitchParam(
    val url: String,
    val networkIdDiscriminant: Byte
) : Parcelable {

    fun toGateway(): Gateway = Gateway.init(
        url = url,
        networkId = NetworkId.init(discriminant = networkIdDiscriminant.toUByte())
    )

    companion object {
        fun from(gateway: Gateway) = GatewayToSwitchParam(
            url = gateway.string,
            networkIdDiscriminant = gateway.network.id.discriminant.toByte()
        )
    }
}

private class OptionalGatewayParamType : NavType<GatewayToSwitchParam>(
    isNullableAllowed = true
) {
    override fun get(bundle: Bundle, key: String): GatewayToSwitchParam? {
        return BundleCompat.getParcelable(
            bundle,
            key,
            GatewayToSwitchParam::class.java
        )
    }

    override fun parseValue(value: String): GatewayToSwitchParam {
        return Serializer.kotlinxSerializationJson.decodeFromString(value)
    }

    override fun put(bundle: Bundle, key: String, value: GatewayToSwitchParam) {
        bundle.putParcelable(key, value)
    }
}

private fun requiresHorizontalTransition(arguments: Bundle?): Boolean {
    arguments ?: return false
    val requestSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arguments.getSerializable(ARG_REQUEST_SOURCE, CreateAccountRequestSource::class.java)
    } else {
        BundleCompat.getSerializable(arguments, ARG_REQUEST_SOURCE, CreateAccountRequestSource::class.java)
    }
    return requestSource?.isFirstTime() ?: false
}
