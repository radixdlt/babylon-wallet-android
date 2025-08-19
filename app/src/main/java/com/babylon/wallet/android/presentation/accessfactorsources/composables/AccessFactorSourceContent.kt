@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.accessfactorsources.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.LabelType
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.usecases.accessfactorsources.AccessOffDeviceMnemonicFactorSourceUseCase.SeedPhraseValidity
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceDelegate
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcePurpose
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourceSkipOption
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.ui.PreviewBackgroundType
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.PinTextField
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.babylon.wallet.android.utils.Constants.ARCULUS_PIN_LENGTH
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.ArculusCardFactorSource
import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.OffDeviceMnemonicFactorSource
import com.radixdlt.sargon.PasswordFactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.delay

@Composable
fun AccessDeviceFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: DeviceFactorSource?,
    isRetryEnabled: Boolean,
    skipOption: AccessFactorSourceSkipOption,
    onRetryClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    AccessFactorSourceContent(
        modifier = modifier,
        purpose = purpose,
        factorSource = factorSource?.asGeneral(),
        factorSourceKind = FactorSourceKind.DEVICE,
        factorActions = {
            AccessContentRetryButton(
                modifier = Modifier
                    .padding(bottom = RadixTheme.dimensions.paddingDefault),
                isEnabled = isRetryEnabled,
                onClick = onRetryClick
            )

            SkipOption(
                skipOption = skipOption,
                onClick = onSkipClick
            )
        }
    )
}

@Composable
fun AccessLedgerHardwareWalletFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: LedgerHardwareWalletFactorSource?,
    isRetryEnabled: Boolean,
    skipOption: AccessFactorSourceSkipOption,
    onRetryClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    AccessFactorSourceContent(
        modifier = modifier,
        purpose = purpose,
        factorSource = factorSource?.asGeneral(),
        factorSourceKind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
        factorActions = {
            AccessContentRetryButton(
                modifier = Modifier
                    .padding(bottom = RadixTheme.dimensions.paddingDefault),
                isEnabled = isRetryEnabled,
                onClick = onRetryClick
            )

            SkipOption(
                skipOption = skipOption,
                onClick = onSkipClick
            )
        }
    )
}

@Composable
fun AccessArculusCardFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: ArculusCardFactorSource?,
    pinState: AccessFactorSourceDelegate.State.ArculusPinState,
    onPinChange: (String) -> Unit,
    skipOption: AccessFactorSourceSkipOption,
    onSkipClick: () -> Unit,
    onConfirmClick: () -> Unit
) {
    AccessFactorSourceContent(
        modifier = modifier,
        purpose = purpose,
        factorSource = factorSource?.asGeneral(),
        factorSourceKind = FactorSourceKind.ARCULUS_CARD,
        factorActions = {
            val isPinRequired = remember(purpose) {
                purpose == AccessFactorSourcePurpose.SignatureRequest ||
                    purpose == AccessFactorSourcePurpose.ProvingOwnership
            }

            if (isPinRequired) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val focusRequester = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current

                    LaunchedEffect(Unit) {
                        delay(300)
                        focusRequester.requestFocus()
                    }

                    PinTextField(
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        textFieldModifier = Modifier.focusRequester(focusRequester),
                        title = "Enter card PIN", // TODO crowdin
                        pinValue = pinState.input,
                        pinLength = ARCULUS_PIN_LENGTH,
                        onPinChange = onPinChange
                    )

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                            .padding(bottom = RadixTheme.dimensions.paddingDefault),
                        text = stringResource(R.string.common_confirm),
                        enabled = pinState.isConfirmButtonEnabled,
                        onClick = {
                            focusManager.clearFocus()
                            onConfirmClick()
                        }
                    )
                }
            }

            SkipOption(
                skipOption = skipOption,
                onClick = onSkipClick
            )
        }
    )
}

@Composable
fun AccessPasswordFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: PasswordFactorSource?,
    passwordState: AccessFactorSourceDelegate.State.PasswordState,
    skipOption: AccessFactorSourceSkipOption,
    onPasswordTyped: (String) -> Unit,
    onSkipClick: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    AccessFactorSourceContent(
        modifier = modifier,
        purpose = purpose,
        factorSource = factorSource?.asGeneral(),
        factorSourceKind = FactorSourceKind.PASSWORD,
        factorActions = {
            RadixTextField(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                    .padding(bottom = RadixTheme.dimensions.paddingLarge),
                value = passwordState.input,
                onValueChanged = onPasswordTyped,
                error = if (passwordState.isPasswordInvalidErrorVisible) {
                    stringResource(R.string.factorSourceActions_password_incorrect)
                } else {
                    null
                },
                leftLabel = LabelType.Default(value = stringResource(R.string.factorSources_card_passwordTitle)),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            isPasswordVisible = !isPasswordVisible
                        },
                        colors = IconButtonColors(
                            contentColor = RadixTheme.colors.iconTertiary,
                            containerColor = Color.Transparent,
                            disabledContentColor = RadixTheme.colors.iconTertiary,
                            disabledContainerColor = Color.Transparent
                        )
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isPasswordVisible) {
                                    com.babylon.wallet.android.designsystem.R.drawable.ic_hide
                                } else {
                                    com.babylon.wallet.android.designsystem.R.drawable.ic_show
                                }
                            ),
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )

            SkipOption(
                skipOption = skipOption,
                onClick = onSkipClick
            )
        }
    )
}

@Composable
fun AccessOffDeviceMnemonicFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: OffDeviceMnemonicFactorSource?,
    seedPhraseInputState: AccessFactorSourceDelegate.State.SeedPhraseInputState,
    skipOption: AccessFactorSourceSkipOption,
    onWordChanged: (Int, String) -> Unit,
    onFocusedWordChanged: (Int) -> Unit,
    onConfirmed: () -> Unit,
    onSkipClick: () -> Unit,
) {
    AccessFactorSourceContent(
        modifier = modifier,
        purpose = purpose,
        factorSource = factorSource?.asGeneral(),
        factorSourceKind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
        factorActions = {
            SecureScreen()

            SeedPhraseInputForm(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                seedPhraseWords = seedPhraseInputState.inputWords,
                bip39Passphrase = "",
                onWordChanged = onWordChanged,
                onPassphraseChanged = {},
                onFocusedWordIndexChanged = onFocusedWordChanged,
                showAdvancedMode = false,
                initiallyFocusedIndex = 0
            )

            AnimatedVisibility(
                visible = seedPhraseInputState.errorInSeedPhrase
            ) {
                WarningText(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(bottom = RadixTheme.dimensions.paddingDefault),
                    text = AnnotatedString(
                        text = when (seedPhraseInputState.seedPhraseValidity) {
                            SeedPhraseValidity.InvalidMnemonic -> stringResource(R.string.factorSourceActions_offDeviceMnemonic_invalid)
                            SeedPhraseValidity.WrongMnemonic -> stringResource(R.string.factorSourceActions_offDeviceMnemonic_wrong)
                            else -> ""
                        }
                    ),
                    contentColor = RadixTheme.colors.error,
                    textStyle = RadixTheme.typography.body2HighImportance
                )
            }

            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .padding(bottom = RadixTheme.dimensions.paddingDefault),
                text = stringResource(R.string.common_confirm),
                enabled = seedPhraseInputState.isConfirmButtonEnabled,
                onClick = onConfirmed
            )

            SkipOption(
                skipOption = skipOption,
                onClick = onSkipClick
            )
        }
    )
}

@Composable
private fun <F : FactorSource> AccessFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSourceKind: FactorSourceKind,
    factorSource: F?,
    factorActions: @Composable ColumnScope.(F) -> Unit
) {
    AccessContent(
        modifier = modifier.fillMaxWidth(),
        title = when (purpose) {
            AccessFactorSourcePurpose.SignatureRequest -> stringResource(R.string.factorSourceActions_signature_title)
            AccessFactorSourcePurpose.ProvingOwnership -> stringResource(R.string.factorSourceActions_proveOwnership_title)
            AccessFactorSourcePurpose.DerivingAccounts -> stringResource(R.string.factorSourceActions_deriveAccounts_title)
            AccessFactorSourcePurpose.UpdatingFactorConfig -> stringResource(R.string.factorSourceActions_updatingFactorConfig_title)
            AccessFactorSourcePurpose.CreatingAccount -> stringResource(R.string.authorization_createAccount_title)
            AccessFactorSourcePurpose.CreatingPersona -> stringResource(R.string.authorization_createPersona_title)
            AccessFactorSourcePurpose.SpotCheck -> stringResource(R.string.factorSourceActions_spotCheck_title)
        },
        message = factorSourceKind.message(purpose),
        content = {
            val card = remember(factorSource) {
                factorSource?.toFactorSourceCard(
                    includeDescription = false,
                    includeLastUsedOn = false
                )
            }
            if (card != null) {
                FactorSourceCardView(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                    item = card,
                    containerColor = if (RadixTheme.config.isDarkTheme) {
                        RadixTheme.colors.backgroundTertiary
                    } else {
                        RadixTheme.colors.background
                    }
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }

            if (factorSource != null) {
                factorActions(factorSource)
            }
        }
    )
}

@Composable
private fun FactorSourceKind.message(purpose: AccessFactorSourcePurpose): AnnotatedString = when (this) {
    FactorSourceKind.DEVICE -> when (purpose) {
        AccessFactorSourcePurpose.SignatureRequest -> stringResource(R.string.factorSourceActions_device_signMessage)
        AccessFactorSourcePurpose.ProvingOwnership,
        AccessFactorSourcePurpose.UpdatingFactorConfig,
        AccessFactorSourcePurpose.DerivingAccounts,
        AccessFactorSourcePurpose.CreatingAccount,
        AccessFactorSourcePurpose.CreatingPersona,
        AccessFactorSourcePurpose.SpotCheck -> stringResource(R.string.factorSourceActions_device_message)
    }

    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> when (purpose) {
        AccessFactorSourcePurpose.SignatureRequest -> stringResource(R.string.factorSourceActions_ledger_signMessage)
        AccessFactorSourcePurpose.ProvingOwnership -> stringResource(R.string.factorSourceActions_ledger_message)
        AccessFactorSourcePurpose.UpdatingFactorConfig,
        AccessFactorSourcePurpose.DerivingAccounts,
        AccessFactorSourcePurpose.CreatingAccount,
        AccessFactorSourcePurpose.CreatingPersona,
        AccessFactorSourcePurpose.SpotCheck -> stringResource(R.string.factorSourceActions_ledger_deriveKeysMessage)
    }

    FactorSourceKind.ARCULUS_CARD -> when (purpose) {
        AccessFactorSourcePurpose.SignatureRequest -> stringResource(R.string.factorSourceActions_arculus_signMessage)
        AccessFactorSourcePurpose.ProvingOwnership -> stringResource(R.string.factorSourceActions_arculus_message)
        AccessFactorSourcePurpose.UpdatingFactorConfig,
        AccessFactorSourcePurpose.DerivingAccounts,
        AccessFactorSourcePurpose.CreatingAccount,
        AccessFactorSourcePurpose.CreatingPersona,
        AccessFactorSourcePurpose.SpotCheck -> stringResource(R.string.factorSourceActions_arculus_deriveKeysMessage)
    }

    FactorSourceKind.OFF_DEVICE_MNEMONIC -> when (purpose) {
        AccessFactorSourcePurpose.SignatureRequest -> stringResource(R.string.factorSourceActions_offDeviceMnemonic_signMessage)
        AccessFactorSourcePurpose.ProvingOwnership,
        AccessFactorSourcePurpose.UpdatingFactorConfig,
        AccessFactorSourcePurpose.DerivingAccounts,
        AccessFactorSourcePurpose.CreatingAccount,
        AccessFactorSourcePurpose.CreatingPersona,
        AccessFactorSourcePurpose.SpotCheck -> stringResource(R.string.factorSourceActions_offDeviceMnemonic_message)
    }

    FactorSourceKind.PASSWORD -> when (purpose) {
        AccessFactorSourcePurpose.SignatureRequest -> stringResource(R.string.factorSourceActions_password_signMessage)
        AccessFactorSourcePurpose.ProvingOwnership,
        AccessFactorSourcePurpose.UpdatingFactorConfig,
        AccessFactorSourcePurpose.DerivingAccounts,
        AccessFactorSourcePurpose.CreatingAccount,
        AccessFactorSourcePurpose.CreatingPersona,
        AccessFactorSourcePurpose.SpotCheck -> stringResource(R.string.factorSourceActions_password_message)
    }
}.formattedSpans(SpanStyle(fontWeight = FontWeight.Bold))

@Composable
private fun SkipOption(
    modifier: Modifier = Modifier,
    skipOption: AccessFactorSourceSkipOption,
    onClick: () -> Unit
) {
    when (skipOption) {
        AccessFactorSourceSkipOption.CanSkipFactor -> RadixTextButton(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .height(50.dp),
            text = stringResource(R.string.factorSourceActions_useDifferentFactor),
            onClick = onClick
        )

        AccessFactorSourceSkipOption.CanIgnoreFactor -> RadixTextButton(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .height(50.dp),
            text = stringResource(R.string.factorSourceActions_ignore),
            onClick = onClick
        )

        AccessFactorSourceSkipOption.None -> {}
    }
}

@Preview(showBackground = true)
@Composable
fun UnknownFactorSourcePreview() {
    RadixWalletPreviewTheme {
        AccessDeviceFactorSourceContent(
            purpose = AccessFactorSourcePurpose.SignatureRequest,
            factorSource = null,
            isRetryEnabled = false,
            skipOption = AccessFactorSourceSkipOption.None,
            onSkipClick = {},
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UnknownFactorSourcePreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        AccessDeviceFactorSourceContent(
            purpose = AccessFactorSourcePurpose.SignatureRequest,
            factorSource = null,
            isRetryEnabled = false,
            skipOption = AccessFactorSourceSkipOption.None,
            onSkipClick = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun PreviewLight(
    @PreviewParameter(AccessFactorSourcePreviewParameterProvider::class) sample: Pair<AccessFactorSourcePurpose, FactorSource>
) {
    RadixWalletPreviewTheme {
        PreviewContent(
            purpose = sample.first,
            factorSource = sample.second
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun PreviewDark(
    @PreviewParameter(AccessFactorSourcePreviewParameterProvider::class) sample: Pair<AccessFactorSourcePurpose, FactorSource>
) {
    RadixWalletPreviewTheme(
        enableDarkTheme = true,
        backgroundType = PreviewBackgroundType.PRIMARY
    ) {
        PreviewContent(
            purpose = sample.first,
            factorSource = sample.second
        )
    }
}

@Composable
@UsesSampleValues
private fun PreviewContent(
    purpose: AccessFactorSourcePurpose,
    factorSource: FactorSource
) {
    val skipOption = remember(purpose) {
        if (purpose == AccessFactorSourcePurpose.SpotCheck) {
            AccessFactorSourceSkipOption.CanIgnoreFactor
        } else {
            AccessFactorSourceSkipOption.CanSkipFactor
        }
    }

    when (factorSource) {
        is FactorSource.Device -> AccessDeviceFactorSourceContent(
            purpose = purpose,
            factorSource = factorSource.value,
            isRetryEnabled = false,
            skipOption = skipOption,
            onSkipClick = {},
            onRetryClick = {}
        )

        is FactorSource.Ledger -> AccessLedgerHardwareWalletFactorSourceContent(
            purpose = purpose,
            factorSource = factorSource.value,
            isRetryEnabled = false,
            skipOption = skipOption,
            onSkipClick = {},
            onRetryClick = {}
        )

        is FactorSource.ArculusCard -> AccessArculusCardFactorSourceContent(
            purpose = purpose,
            factorSource = factorSource.value,
            pinState = AccessFactorSourceDelegate.State.ArculusPinState(),
            skipOption = skipOption,
            onPinChange = {},
            onSkipClick = {},
            onConfirmClick = {}
        )

        is FactorSource.Password -> {
            var password by remember { mutableStateOf("") }
            AccessPasswordFactorSourceContent(
                purpose = purpose,
                factorSource = factorSource.value,
                skipOption = skipOption,
                passwordState = AccessFactorSourceDelegate.State.PasswordState(
                    input = password,
                    isPasswordInvalidErrorVisible = false
                ),
                onPasswordTyped = {
                    password = it
                },
                onSkipClick = {},
            )
        }

        is FactorSource.OffDeviceMnemonic -> {
            val scope = rememberCoroutineScope()
            val delegate = remember {
                SeedPhraseInputDelegate(scope).apply {
                    setSeedPhraseSize(factorSource.value.hint.wordCount)
                }
            }
            val state by delegate.state.collectAsState()

            AccessOffDeviceMnemonicFactorSourceContent(
                purpose = purpose,
                factorSource = factorSource.value,
                skipOption = skipOption,
                seedPhraseInputState = AccessFactorSourceDelegate.State.SeedPhraseInputState(
                    delegateState = state,
                    seedPhraseValidity = SeedPhraseValidity.WrongMnemonic
                ),
                onWordChanged = delegate::onWordChanged,
                onFocusedWordChanged = {},
                onConfirmed = {},
                onSkipClick = {},
            )
        }
    }
}

@UsesSampleValues
class AccessFactorSourcePreviewParameterProvider :
    PreviewParameterProvider<Pair<AccessFactorSourcePurpose, FactorSource>> {

    private val samples: List<Pair<AccessFactorSourcePurpose, FactorSource>>

    init {
        val factorSources = listOf(
            DeviceFactorSource.sample().asGeneral(),
            LedgerHardwareWalletFactorSource.sample().asGeneral(),
            ArculusCardFactorSource.sample().asGeneral(),
            OffDeviceMnemonicFactorSource.sample.other().asGeneral(),
            PasswordFactorSource.sample().asGeneral()
        )

        samples = factorSources.flatMap { factorSource ->
            AccessFactorSourcePurpose.entries.map { purpose ->
                purpose to factorSource
            }
        }
    }

    override val values = samples.asSequence()
}
