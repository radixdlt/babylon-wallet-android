package com.babylon.wallet.android.presentation.ui.composables.card

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
interface EventBusComposableEntryPointI {

    fun appEventBus(): AppEventBus
}

@ActivityRetainedScoped
class EventBusComposableEntryPoint @Inject constructor(
    @ApplicationContext context: Context
) : EventBusComposableEntryPointI {

    private val provider = EntryPoints.get(context, EventBusComposableEntryPointI::class.java)

    override fun appEventBus(): AppEventBus = provider.appEventBus()
}

val eventBusComposableEntryPointMock = object : EventBusComposableEntryPointI {
    override fun appEventBus(): AppEventBus = AppEventBusImpl()
}

@Suppress("CompositionLocalAllowlist")
val LocalEventBusComposableEntryPoint = compositionLocalOf<EventBusComposableEntryPointI> {
    error("No FactorSourceCardViewEntryPoint provided")
}

@Composable
fun ProvideMockFactorSourceCardViewEntryPoint(
    entryPoint: EventBusComposableEntryPointI = eventBusComposableEntryPointMock,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalEventBusComposableEntryPoint provides entryPoint) {
        content()
    }
}
