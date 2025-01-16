package com.babylon.wallet.android.presentation.accessfactorsources.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextAlign
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
import com.babylon.wallet.android.presentation.accessfactorsources.access.AccessFactorSourceDelegate
import com.babylon.wallet.android.presentation.accessfactorsources.models.AccessFactorSourcePurpose
import com.babylon.wallet.android.presentation.accessfactorsources.models.AccessFactorSourcePurpose.ProvingOwnership
import com.babylon.wallet.android.presentation.accessfactorsources.models.AccessFactorSourcePurpose.SignatureRequest
import com.babylon.wallet.android.presentation.accessfactorsources.models.AccessFactorSourcePurpose.UpdatingFactorConfig
import com.babylon.wallet.android.presentation.common.seedphrase.SeedPhraseInputDelegate
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.SeedPhraseInputForm
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardView
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
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

@Composable
fun AccessDeviceFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: DeviceFactorSource?,
    isAccessingFactor: Boolean,
    canUseDifferentFactor: Boolean,
    onRetryClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    AccessFactorSourceContent(
        modifier = modifier,
        purpose = purpose,
        factorSource = factorSource?.asGeneral(),
        factorSourceKind = FactorSourceKind.DEVICE,
        factorActions = {
            RetryButton(
                modifier = Modifier
                    .padding(bottom = RadixTheme.dimensions.paddingDefault),
                isEnabled = !isAccessingFactor,
                onClick = onRetryClick
            )

            if (canUseDifferentFactor) {
                UseDifferentFactor(onClick = onSkipClick)
            }
        }
    )
}

@Composable
fun AccessLedgerHardwareWalletFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: LedgerHardwareWalletFactorSource?,
    isAccessingFactor: Boolean,
    canUseDifferentFactor: Boolean,
    onRetryClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    AccessFactorSourceContent(
        modifier = modifier,
        purpose = purpose,
        factorSource = factorSource?.asGeneral(),
        factorSourceKind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
        factorActions = {
            RetryButton(
                modifier = Modifier
                    .padding(bottom = RadixTheme.dimensions.paddingDefault),
                isEnabled = !isAccessingFactor,
                onClick = onRetryClick
            )

            if (canUseDifferentFactor) {
                UseDifferentFactor(onClick = onSkipClick)
            }
        }
    )
}

@Composable
fun AccessArculusCardFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: ArculusCardFactorSource?,
    canUseDifferentFactor: Boolean,
    onSkipClick: () -> Unit
) {
    AccessFactorSourceContent(
        modifier = modifier,
        purpose = purpose,
        factorSource = factorSource?.asGeneral(),
        factorSourceKind = FactorSourceKind.ARCULUS_CARD,
        factorActions = {
            if (canUseDifferentFactor) {
                UseDifferentFactor(onClick = onSkipClick)
            }
        }
    )
}

@Composable
fun AccessPasswordFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: PasswordFactorSource?,
    passwordState: AccessFactorSourceDelegate.State.PasswordState,
    onPasswordTyped: (String) -> Unit,
    canUseDifferentFactor: Boolean,
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
                            contentColor = RadixTheme.colors.gray3,
                            containerColor = Color.Transparent,
                            disabledContentColor = RadixTheme.colors.gray3,
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

            if (canUseDifferentFactor) {
                UseDifferentFactor(onClick = onSkipClick)
            }
        }
    )
}

@Composable
fun AccessOffDeviceMnemonicFactorSourceContent(
    modifier: Modifier = Modifier,
    purpose: AccessFactorSourcePurpose,
    factorSource: OffDeviceMnemonicFactorSource?,
    seedPhraseInputState: AccessFactorSourceDelegate.State.SeedPhraseInputState,
    canUseDifferentFactor: Boolean,
    onWordChanged: (Int, String) -> Unit,
    onConfirmed: () -> Unit,
    onSkipClick: () -> Unit,
) {
    AccessFactorSourceContent(
        modifier = modifier,
        purpose = purpose,
        factorSource = factorSource?.asGeneral(),
        factorSourceKind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
        factorActions = {
            var focusedWordIndex by remember {
                mutableStateOf<Int?>(null)
            }

            SeedPhraseInputForm(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                seedPhraseWords = seedPhraseInputState.inputWords,
                bip39Passphrase = "",
                onWordChanged = onWordChanged,
                onPassphraseChanged = {},
                onFocusedWordIndexChanged = {
                    focusedWordIndex = it
                },
                showAdvancedMode = false,
                initiallyFocusedIndex = 0
            )

            AnimatedVisibility(
                visible = seedPhraseInputState.isSeedPhraseInvalidErrorVisible
            ) {
                WarningText(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .padding(bottom = RadixTheme.dimensions.paddingDefault),
                    text = AnnotatedString(
                        text = stringResource(R.string.factorSourceActions_offDeviceMnemonic_incorrect)
                    ),
                    contentColor = RadixTheme.colors.red1,
                    textStyle = RadixTheme.typography.body2HighImportance
                )
            }

            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                    .padding(bottom = RadixTheme.dimensions.paddingDefault),
                text = "Confirm",
                enabled = seedPhraseInputState.isConfirmButtonEnabled,
                onClick = onConfirmed
            )

            if (canUseDifferentFactor) {
                UseDifferentFactor(onClick = onSkipClick)
            }
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
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                .size(81.dp),
            painter = painterResource(
                id = com.babylon.wallet.android.designsystem.R.drawable.ic_security_key
            ),
            contentDescription = null,
            tint = RadixTheme.colors.gray3
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = when (purpose) {
                SignatureRequest -> stringResource(R.string.factorSourceActions_signature_title)
                ProvingOwnership -> stringResource(R.string.factorSourceActions_proveOwnership_title)
                UpdatingFactorConfig -> stringResource(R.string.factorSourceActions_updatingFactorConfig_title)
            },
            color = RadixTheme.colors.gray1,
            style = RadixTheme.typography.title,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
            text = factorSourceKind.message(purpose),
            color = RadixTheme.colors.gray1,
            style = RadixTheme.typography.body1Regular,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

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
                item = card
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        }

        if (factorSource != null) {
            factorActions(factorSource)
        }
    }
}

@Composable
private fun FactorSourceKind.message(purpose: AccessFactorSourcePurpose): AnnotatedString = when (this) {
    FactorSourceKind.DEVICE -> when (purpose) {
        SignatureRequest -> stringResource(R.string.factorSourceActions_device_signMessage)
        ProvingOwnership, UpdatingFactorConfig -> stringResource(R.string.factorSourceActions_device_message)
    }

    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> when (purpose) {
        SignatureRequest -> stringResource(R.string.factorSourceActions_ledger_signMessage)
        ProvingOwnership -> stringResource(R.string.factorSourceActions_ledger_message)
        UpdatingFactorConfig -> stringResource(R.string.factorSourceActions_ledger_deriveKeysMessage)
    }

    FactorSourceKind.ARCULUS_CARD -> when (purpose) {
        SignatureRequest -> stringResource(R.string.factorSourceActions_arculus_signMessage)
        ProvingOwnership -> stringResource(R.string.factorSourceActions_arculus_message)
        UpdatingFactorConfig -> stringResource(R.string.factorSourceActions_arculus_deriveKeysMessage)
    }

    FactorSourceKind.OFF_DEVICE_MNEMONIC -> when (purpose) {
        SignatureRequest -> stringResource(R.string.factorSourceActions_offDeviceMnemonic_signMessage)
        ProvingOwnership, UpdatingFactorConfig -> stringResource(R.string.factorSourceActions_offDeviceMnemonic_message)
    }

    FactorSourceKind.PASSWORD -> when (purpose) {
        SignatureRequest -> stringResource(R.string.factorSourceActions_password_signMessage)
        ProvingOwnership, UpdatingFactorConfig -> stringResource(R.string.factorSourceActions_password_message)
    }

    FactorSourceKind.TRUSTED_CONTACT -> ""
    FactorSourceKind.SECURITY_QUESTIONS -> ""
}.formattedSpans(SpanStyle(fontWeight = FontWeight.Bold))

@Composable
private fun RetryButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    RadixTextButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
            .height(50.dp),
        text = stringResource(R.string.common_retry),
        enabled = isEnabled,
        onClick = onClick
    )
}

@Composable
private fun UseDifferentFactor(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    RadixTextButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
            .height(50.dp),
        text = stringResource(R.string.factorSourceActions_useDifferentFactor),
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
fun UnknownFactorSourcePreview() {
    RadixWalletPreviewTheme {
        AccessDeviceFactorSourceContent(
            purpose = SignatureRequest,
            factorSource = null,
            isAccessingFactor = false,
            canUseDifferentFactor = false,
            onSkipClick = {},
            onRetryClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun Preview(
    @PreviewParameter(AccessFactorSourcePreviewParameterProvider::class) sample: Pair<AccessFactorSourcePurpose, FactorSource>
) {
    RadixWalletPreviewTheme {
        when (val factorSource = sample.second) {
            is FactorSource.Device -> AccessDeviceFactorSourceContent(
                purpose = sample.first,
                factorSource = factorSource.value,
                isAccessingFactor = false,
                canUseDifferentFactor = true,
                onSkipClick = {},
                onRetryClick = {}
            )

            is FactorSource.Ledger -> AccessLedgerHardwareWalletFactorSourceContent(
                purpose = sample.first,
                factorSource = factorSource.value,
                isAccessingFactor = false,
                canUseDifferentFactor = true,
                onSkipClick = {},
                onRetryClick = {}
            )

            is FactorSource.ArculusCard -> AccessArculusCardFactorSourceContent(
                purpose = sample.first,
                factorSource = factorSource.value,
                canUseDifferentFactor = true,
                onSkipClick = {},
            )

            is FactorSource.Password -> {
                var password by remember { mutableStateOf("") }
                AccessPasswordFactorSourceContent(
                    purpose = sample.first,
                    factorSource = factorSource.value,
                    canUseDifferentFactor = true,
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
                    purpose = sample.first,
                    factorSource = factorSource.value,
                    canUseDifferentFactor = true,
                    seedPhraseInputState = AccessFactorSourceDelegate.State.SeedPhraseInputState(
                        delegateState = state,
                        isSeedPhraseInvalidErrorVisible = true
                    ),
                    onWordChanged = delegate::onWordChanged,
                    onConfirmed = {},
                    onSkipClick = {},
                )
            }

            is FactorSource.SecurityQuestions -> {}
            is FactorSource.TrustedContact -> {}
        }
    }
}


@UsesSampleValues
class AccessFactorSourcePreviewParameterProvider : PreviewParameterProvider<Pair<AccessFactorSourcePurpose, FactorSource>> {

    private val samples: List<Pair<AccessFactorSourcePurpose, FactorSource>>

    init {
        val factorSources = listOf(
            DeviceFactorSource.sample().asGeneral(),
            LedgerHardwareWalletFactorSource.sample().asGeneral(),
            ArculusCardFactorSource.sample().asGeneral(),
            OffDeviceMnemonicFactorSource.sample.other().asGeneral(),
            PasswordFactorSource.sample().asGeneral()
        )

        samples = AccessFactorSourcePurpose.entries.flatMap { purpose ->
            factorSources.map { factorSource ->
                purpose to factorSource
            }
        }
    }

    override val values = samples.asSequence()
}
