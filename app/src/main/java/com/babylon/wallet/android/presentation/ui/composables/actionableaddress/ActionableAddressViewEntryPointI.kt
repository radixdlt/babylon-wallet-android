package com.babylon.wallet.android.presentation.ui.composables.actionableaddress

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.AppEventBusImpl
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActionableAddressViewEntryPointI {

    fun appEventBus(): AppEventBus
}

@ActivityRetainedScoped
class ActionableAddressViewEntryPoint @Inject constructor(
    @ApplicationContext context: Context
) : ActionableAddressViewEntryPointI {

    private val provider = EntryPoints.get(context, ActionableAddressViewEntryPointI::class.java)

    override fun appEventBus(): AppEventBus = provider.appEventBus()
}

val actionableAddressViewEntryPointMock = object : ActionableAddressViewEntryPointI {
    override fun appEventBus(): AppEventBus = AppEventBusImpl()
}

@Suppress("CompositionLocalAllowlist")
val LocalActionableAddressViewEntryPoint = compositionLocalOf<ActionableAddressViewEntryPointI> {
    error("No ActionableAddressViewEntryPoint provided")
}

@Composable
fun ProvideMockActionableAddressViewEntryPoint(
    entryPoint: ActionableAddressViewEntryPointI = actionableAddressViewEntryPointMock,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalActionableAddressViewEntryPoint provides entryPoint) {
        content()
    }
}
