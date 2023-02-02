package com.babylon.wallet.android.presentation.navigation

interface Destination {
    /**
     * Defines a specific route this destination belongs to.
     * Route is a String that defines the path to your composable.
     * You can think of it as an implicit deep link that leads to a specific destination.
     * Each destination should have a unique route.
     */
    val route: String
    val args: String
}

sealed class Screen(override val route: String, override val args: String = "") : Destination {

    object OnboardingDestination : Screen("onboarding_route")
    object WalletDestination : Screen("wallet_route")
    object SettingsDestination : Screen("settings_route")
    object SettingsAllDestination : Screen("settings_all_route")
    object SettingsEditGatewayApiDestination : Screen("settings_edit_gateway_api_route")
    object AccountDestination : Screen("account_route")
    object ChooseAccountsCompleteDestination : Screen("choose_accounts_completion_route")

    fun routeWithArgs(vararg args: Any): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }

    companion object {
        const val ARG_ACCOUNT_ID = "arg_account_id"
        const val ARG_ACCOUNT_NAME = "arg_account_name"
        const val ARG_DAPP_NAME = "arg_dapp_name"
        const val ARG_HAS_PROFILE = "arg_has_profile"
    }
}
