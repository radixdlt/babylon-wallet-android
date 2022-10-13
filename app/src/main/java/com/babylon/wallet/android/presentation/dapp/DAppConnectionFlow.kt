package com.babylon.wallet.android.presentation.dapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun DAppConnectionFlow(
//    viewModel: DAppConnectionFlowViewModel = hiltViewModel()
) {

//    val state by viewModel.state.collectAsStateWithLifecycle()

//    when (state) {
//        DAppUiState.RequestScreen -> {
//            DAppConnectionRequestScreen(
//                onCloseClick = {},
//                onContinueClick = {
//
//                },
//                imageUrl = "",
//                labels = listOf(
//                    stringResource(id = R.string.dapp_condition1),
//                    stringResource(id = R.string.dapp_condition2)
//                )
//            )
//        }
//        DAppUiState.LoginScreen -> {
//
//        }
//    }
//    HelloContent(
//        userState = state,
//        onSignOut = viewModel::signOut,
//        onLogin = viewModel::login
//    )
}

// @Composable
// private fun HelloContent(userState: UserState, onLogin: (String) -> Unit, onSignOut: () -> Unit) {
//    Scaffold(
//        modifier = Modifier.padding(Padding.XL.dp),
//        bottomBar = {
//            Button(onClick = onSignOut) {
//                Text(text = stringResource(R.string.sign_out))
//            }
//        }
//    ) {
//        when (userState) {
//            is UserState.LoggedIn -> UserDataContent(user = userState.user)
//            UserState.Registered -> UnlockAccessContent(onLogin)
//            UserState.NotRegistered -> Unit
//        }
//    }
// }
//
//
// @Composable
// private fun UnlockAccessContent(onLogin: (String) -> Unit) {
//    val (pin, onPinUpdate) = remember { mutableStateOf("") }
//
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Text(
//            style = MaterialTheme.typography.h3,
//            text = stringResource(R.string.type_your_pin_text)
//        )
//
//        OutlinedTextField(
//            textStyle = MaterialTheme.typography.h3,
//            value = pin,
//            onValueChange = onPinUpdate,
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//            visualTransformation = PasswordVisualTransformation()
//        )
//
//        Button(
//            onClick = {
//                onLogin(pin)
//                onPinUpdate("")
//            }
//        ) {
//            Text(text = stringResource(R.string.login))
//        }
//    }
// }
//
// @Composable
// private fun UserDataContent(user: User) {
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            style = MaterialTheme.typography.h2,
//            text = stringResource(
//                R.string.hello_user_format_text,
//                "${user.firstName} ${user.lastName}"
//            )
//        )
//        Text(
//            style = MaterialTheme.typography.h4,
//            text = user.phoneNumber
//        )
//        Text(
//            style = MaterialTheme.typography.h4,
//            text = user.email
//        )
//    }
// }
