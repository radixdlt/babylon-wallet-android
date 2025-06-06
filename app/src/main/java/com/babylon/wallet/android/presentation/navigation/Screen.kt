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
    object SettingsEditGatewayApiDestination : Screen("settings_edit_gateway_api_route")

    companion object {
        const val ARG_ACCOUNT_NAME = "arg_account_name"
        const val ARG_HAS_PROFILE = "arg_has_profile"
    }
}
