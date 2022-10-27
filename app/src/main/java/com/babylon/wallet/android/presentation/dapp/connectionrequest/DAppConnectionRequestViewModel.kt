// TODO this might be reverted later on
// package com.babylon.wallet.android.presentation.dapp.connectionrequest
//
// import androidx.compose.runtime.getValue
// import androidx.compose.runtime.mutableStateOf
// import androidx.compose.runtime.setValue
// import androidx.lifecycle.ViewModel
// import androidx.lifecycle.viewModelScope
// import com.babylon.wallet.android.domain.dapp.ConnectDAppUseCase
// import com.babylon.wallet.android.presentation.navigation.Screen
// import dagger.hilt.android.lifecycle.HiltViewModel
// import kotlinx.coroutines.launch
// import javax.inject.Inject
//
// @HiltViewModel
// class DAppConnectionRequestViewModel @Inject constructor(
//    private val connectDAppUseCase: ConnectDAppUseCase
// ) : ViewModel() {
//
//    var state by mutableStateOf(ConnectionRequestUiState())
//        private set
//
//    init {
//        viewModelScope.launch {
//            val result = connectDAppUseCase("")
//            state = state.copy(
//                labels = result.payloadFields,
//                destination = Screen.DAppChoosePersonaDestination
//            )
//        }
//    }
// }
//
// data class ConnectionRequestUiState(
//    val labels: List<String>? = null,
//    val destination: Screen? = null
// )
