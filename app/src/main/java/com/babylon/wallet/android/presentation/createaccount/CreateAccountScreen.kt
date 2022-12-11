package com.babylon.wallet.android.presentation.createaccount

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import kotlinx.coroutines.Dispatchers
import rdx.works.profile.data.repository.ProfileRepositoryImpl
import rdx.works.profile.domain.CreateAccountUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.domain.GetMnemonicUseCase

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun CreateAccountScreen(
    viewModel: CreateAccountViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueClick: (
        accountId: String,
        accountName: String,
        profileExists: Boolean
    ) -> Unit = { _: String, _: String, _: Boolean -> },
) {

    val state = viewModel.state

    if (state.complete) {
        onContinueClick(state.accountId, state.accountName, state.profileExists)
    } else if (state.loading) {
        FullscreenCircularProgressContent()
    } else {
        val accountName = viewModel.accountName.collectAsStateWithLifecycle().value
        val buttonEnabled = viewModel.buttonEnabled.collectAsStateWithLifecycle().value

        CreateAccountContent(
            onAccountNameChange = viewModel::onAccountNameChange,
            onAccountCreateClick = viewModel::onAccountCreateClick,
            accountName = accountName,
            buttonEnabled = buttonEnabled,
            onBackClick = onBackClick,
            modifier = modifier
        )
    }
}

@Composable
fun CreateAccountContent(
    onAccountNameChange: (String) -> Unit,
    onAccountCreateClick: () -> Unit,
    accountName: String,
    buttonEnabled: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .systemBarsPadding()
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxSize()
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_close),
                tint = RadixTheme.colors.gray1,
                contentDescription = "navigate back"
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = RadixTheme.dimensions.paddingXLarge,
                    vertical = RadixTheme.dimensions.paddingDefault
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.img_account_creation),
                contentDescription = "account_creation_image"
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                text = stringResource(id = R.string.create_new_account),
                style = RadixTheme.typography.header,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(id = R.string.account_creation_text),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(30.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                RadixTextField(
                    modifier = Modifier.fillMaxWidth(),
                    onValueChanged = onAccountNameChange,
                    value = accountName,
                    hint = stringResource(id = R.string.account_name)
                )
                Text(
                    text = stringResource(id = R.string.this_can_be_changed_any_time),
                    style = RadixTheme.typography.body1Regular,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            }
            Spacer(Modifier.weight(1f))
            RadixPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAccountCreateClick,
                enabled = buttonEnabled,
                text = stringResource(id = R.string.continue_button_title)
            )
        }
    }
}

val Context.dataStore by preferencesDataStore("test_preferences")

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun CreateAccountPreview() {
    val dataStore: DataStore<Preferences> = LocalContext.current.dataStore

    val viewModel = CreateAccountViewModel(
        savedStateHandle = SavedStateHandle(),
        profileRepository = ProfileRepositoryImpl(
            dataStore = dataStore,
            defaultDispatcher = Dispatchers.Main
        ),
        generateProfileUseCase = GenerateProfileUseCase(
            getMnemonicUseCase = GetMnemonicUseCase(
                dataStore = dataStore
            ),
            profileRepository = ProfileRepositoryImpl(
                dataStore = dataStore,
                defaultDispatcher = Dispatchers.Main
            )
        ),
        createAccountUseCase = CreateAccountUseCase(
            generateMnemonicUseCase = GetMnemonicUseCase(
                dataStore = dataStore
            ),
            profileRepository = ProfileRepositoryImpl(
                dataStore = dataStore,
                defaultDispatcher = Dispatchers.Main
            )
        )
    )

    BabylonWalletTheme {
        CreateAccountScreen(
            viewModel = viewModel,
            onBackClick = {},
            onContinueClick = { _, _, _ -> }
        )
    }
}
