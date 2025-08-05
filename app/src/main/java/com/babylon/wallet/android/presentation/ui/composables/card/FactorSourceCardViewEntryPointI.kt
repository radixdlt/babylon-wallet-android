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
interface FactorSourceCardViewEntryPointI {

    fun appEventBus(): AppEventBus
}

@ActivityRetainedScoped
class FactorSourceCardViewEntryPoint @Inject constructor(
    @ApplicationContext context: Context
) : FactorSourceCardViewEntryPointI {

    private val provider = EntryPoints.get(context, FactorSourceCardViewEntryPointI::class.java)

    override fun appEventBus(): AppEventBus = provider.appEventBus()
}

val factorSourceCardViewEntryPointMock = object : FactorSourceCardViewEntryPointI {
    override fun appEventBus(): AppEventBus = AppEventBusImpl()
}

@Suppress("CompositionLocalAllowlist")
val LocalFactorSourceCardViewEntryPoint = compositionLocalOf<FactorSourceCardViewEntryPointI> {
    error("No FactorSourceCardViewEntryPoint provided")
}

@Composable
fun ProvideMockFactorSourceCardViewEntryPoint(
    entryPoint: FactorSourceCardViewEntryPointI = factorSourceCardViewEntryPointMock,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalFactorSourceCardViewEntryPoint provides entryPoint) {
        content()
    }
}
