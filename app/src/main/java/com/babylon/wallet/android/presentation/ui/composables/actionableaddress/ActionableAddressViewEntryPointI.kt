package com.babylon.wallet.android.presentation.ui.composables.actionableaddress

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.usecases.VerifyAddressOnLedgerUseCase
import com.babylon.wallet.android.presentation.mocks.LedgerMessengerMock
import com.babylon.wallet.android.presentation.mocks.fakeGetProfileUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActionableAddressViewEntryPointI {

    fun profileUseCase(): GetProfileUseCase

    fun verifyAddressOnLedgerUseCase(): VerifyAddressOnLedgerUseCase

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}

@ActivityRetainedScoped
class ActionableAddressViewEntryPoint @Inject constructor(
    @ApplicationContext context: Context
) : ActionableAddressViewEntryPointI {

    private val useCaseProvider = EntryPoints.get(context, ActionableAddressViewEntryPointI::class.java)

    override fun profileUseCase(): GetProfileUseCase {
        return useCaseProvider.profileUseCase()
    }

    override fun verifyAddressOnLedgerUseCase(): VerifyAddressOnLedgerUseCase {
        return useCaseProvider.verifyAddressOnLedgerUseCase()
    }

    override fun applicationScope(): CoroutineScope {
        return useCaseProvider.applicationScope()
    }
}

val actionableAddressViewEntryPointMock = object : ActionableAddressViewEntryPointI {
    override fun profileUseCase(): GetProfileUseCase {
        return fakeGetProfileUseCase()
    }

    override fun verifyAddressOnLedgerUseCase(): VerifyAddressOnLedgerUseCase {
        return VerifyAddressOnLedgerUseCase(fakeGetProfileUseCase(), LedgerMessengerMock())
    }

    override fun applicationScope(): CoroutineScope {
        return MainScope()
    }
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
