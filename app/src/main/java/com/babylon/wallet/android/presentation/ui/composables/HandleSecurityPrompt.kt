package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.babylon.wallet.android.domain.usecases.securityproblems.SecurityPromptType
import com.babylon.wallet.android.presentation.ui.composables.card.LocalEventBusComposableEntryPoint
import com.babylon.wallet.android.utils.AppEvent
import com.radixdlt.sargon.FactorSourceId
import kotlinx.coroutines.launch

@Composable
fun HandleSecurityPrompt(
    factorSourceId: FactorSourceId?,
    clickEvent: State<SecurityPromptType?>,
    onConsumeUpstream: () -> Unit,
    onConsumed: () -> Unit
) {
    val dependenciesProvider = LocalEventBusComposableEntryPoint.current
    val coroutineScope = rememberCoroutineScope()
    val sendEvent = remember {
        {
                event: AppEvent.FixSecurityIssue ->
            coroutineScope.launch {
                dependenciesProvider.appEventBus().sendEvent(event)
            }
        }
    }

    LaunchedEffect(clickEvent.value) {
        clickEvent.value?.let { securityPromptType ->
            when (securityPromptType) {
                SecurityPromptType.WRITE_DOWN_SEED_PHRASE -> {
                    if (factorSourceId != null) {
                        sendEvent(
                            AppEvent.FixSecurityIssue.WriteDownSeedPhrase(
                                factorSourceId = factorSourceId
                            )
                        )
                    } else {
                        onConsumeUpstream()
                    }
                }

                SecurityPromptType.RECOVERY_REQUIRED -> {
                    if (factorSourceId != null) {
                        sendEvent(
                            AppEvent.FixSecurityIssue.ImportMnemonic(
                                factorSourceId = factorSourceId
                            )
                        )
                    } else {
                        onConsumeUpstream()
                    }
                }

                SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM,
                SecurityPromptType.WALLET_NOT_RECOVERABLE,
                SecurityPromptType.CONFIGURATION_BACKUP_NOT_UPDATED -> onConsumeUpstream()
            }

            onConsumed()
        }
    }
}
