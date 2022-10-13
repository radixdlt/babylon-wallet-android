// package com.babylon.wallet.android.presentation.dapp
//
// import androidx.hilt.navigation.compose.hiltViewModel
// import androidx.navigation.NavGraphBuilder
// import androidx.navigation.NavHostController
// import androidx.navigation.compose.composable
// import androidx.navigation.navigation
// import com.babylon.wallet.android.presentation.navigation.Destination
// import com.babylon.wallet.android.presentation.navigation.Graph
//
// fun NavGraphBuilder.dAppNavGraph(navController: NavHostController) {
//    navigation(
//        startDestination = DAppScreen.ConnectionRequest.route,
//        route = Graph.DAPP
//    ) {
//        composable(route = DAppScreen.ConnectionRequest.route) {
//            DAppConnectionRequestScreen(
//                viewModel = hiltViewModel(),
//                onCloseClick = { navController.popBackStack() },
//                onContinueClick = { navController.navigate(DAppScreen.ChooseLogin.route) },
//                imageUrl = "",
//                labels = listOf()
//            )
//        }
//        composable(route = DAppScreen.ChooseLogin.route) {
//            ChooseDAppLoginScreen(
//                onBackClick = { navController.popBackStack() },
//                onContinueClick = { /*TODO*/ },
//                imageUrl = "",
//                accounts = arrayOf()
//            )
//        }
//    }
// }
//
// sealed class DAppScreen(override val route: String) : Destination {
//    object ConnectionRequest : DAppScreen(route = "dapp_connection_request")
//    object ChooseLogin : DAppScreen(route = "dapp_choose_login")
// }
