package rdx.works.profile.accountextensions

import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import rdx.works.core.InstantGenerator
import rdx.works.core.identifiedArrayListOf
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.extensions.isSignatureRequiredBasedOnDepositRules
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.addAccounts
import rdx.works.profile.data.model.pernetwork.updateThirdPartyDepositSettings
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.TestData
import kotlin.test.assertTrue

class TransferBetweenOwnedAccountsTest {

    private val mnemonicWithPassphrase = MnemonicWithPassphrase(
        mnemonic = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate",
        bip39Passphrase = ""
    )
    private val babylonFactorSource = DeviceFactorSource.babylon(
        mnemonicWithPassphrase, model = TestData.deviceInfo.displayName,
        name = "Samsung"
    )

    var profile = Profile.init(
        id = "BABE1442-3C98-41FF-AFB0-D0F5829B020D",
        deviceInfo = TestData.deviceInfo,
        creationDate = InstantGenerator()
    ).copy(factorSources = identifiedArrayListOf(babylonFactorSource))

    private val defaultNetwork = Radix.Gateway.default.network

    private lateinit var targetAccount: Network.Account

    private val asset1address = "asset1address"
    private val asset2address = "asset2address"
    private val targetAccountWithAsset1 = listOf(asset1address)

    private val acceptAll = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
        depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll
    )

    private val acceptAllAndDenyAsset1 = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
        depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll,
        assetsExceptionList = listOf(
            Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException(
                address = asset1address,
                exceptionRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Deny
            )
        )
    )

    private val denyAll = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
        depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll
    )

    private val denyAllAndAllowAsset1 = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
        depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll,
        assetsExceptionList = listOf(
            Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException(
                address = asset1address,
                exceptionRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Allow
            )
        )
    )

    private val denyAllAndDenyAsset1 = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
        depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll,
        assetsExceptionList = listOf(
            Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException(
                address = asset1address,
                exceptionRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Deny
            )
        )
    )

    private val acceptKnown = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
        depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown
    )

    private val acceptKnownAndAllowAsset1 = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
        depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown,
        assetsExceptionList = listOf(
            Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException(
                address = asset1address,
                exceptionRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Allow
            )
        )
    )

    private val acceptKnownAndDenyAsset1 = Network.Account.OnLedgerSettings.ThirdPartyDeposits(
        depositRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown,
        assetsExceptionList = listOf(
            Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException(
                address = asset1address,
                exceptionRule = Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Deny
            )
        )
    )

    @Before
    fun setUp() {
        val mnemonicRepository = mockk<MnemonicRepository>()
        coEvery { mnemonicRepository() } returns mnemonicWithPassphrase

        targetAccount = Network.Account.initAccountWithBabylonDeviceFactorSource(
            entityIndex = 0,
            displayName = "target account",
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            deviceFactorSource = (profile.factorSources.first() as DeviceFactorSource),
            networkId = defaultNetwork.networkId(),
            appearanceID = 0
        )

        profile = profile.addAccounts(
            accounts = listOf(targetAccount),
            onNetwork = defaultNetwork.networkId()
        )
    }

    @Test
    fun `given accept all, when transfer between user's own accounts, then signature is not required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptAll
        )
        assertFalse(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given deny all, when transfer between user's own accounts, then signature is required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = denyAll
        )
        assertTrue(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given accept all and deny Asset1 rule, when transfer Asset2 between user's own accounts, then signature is not required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptAllAndDenyAsset1
        )
        assertFalse(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset2address))
    }

    @Test
    fun `given accept all and deny Asset1 rule, when transfer Asset1 between user's own accounts, then signature is required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptAllAndDenyAsset1
        )
        assertTrue(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given deny all and allow Asset1 rule, when transfer Asset2 between user's own accounts, then signature is required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = denyAllAndAllowAsset1
        )
        assertTrue(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset2address))
    }

    @Test
    fun `given deny all and allow Asset1 rule, when transfer Asset1 between user's own accounts, then signature is not required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = denyAllAndAllowAsset1
        )
        assertFalse(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given deny all and deny Asset1 rule, when transfer Asset1 between user's own accounts, then signature is required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = denyAllAndDenyAsset1
        )
        assertTrue(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given accept known and target account has not Asset1, when transfer Asset1 from user's own account to user's target account, then signature is required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptKnown
        )
        assertTrue(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given accept known and allow Asset1 rule and target account has not Asset1, when transfer Asset1 from user's own account to user's target account, then signature is not required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptKnownAndAllowAsset1
        )
        assertFalse(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given accept known and deny Asset1 rule and target account has not Asset1, when transfer Asset1 from user's own account to user's target account, then signature is required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptKnownAndDenyAsset1
        )
        assertTrue(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given accept known and target account has Asset1, when transfer Asset1 from user's own account to user's target account, then signature is not required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptKnown
        )
        assertFalse(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address, targetAccountWithAsset1))
    }

    @Test
    fun `given accept known and allow Asset1 rule and target account has Asset1, when transfer Asset1 from user's own account to user's target account, then signature is not required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptKnownAndAllowAsset1
        )
        assertFalse(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given accept known and deny Asset1 rule and target account has Asset1, when transfer Asset1 from user's own account to user's target account, then signature is required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptKnownAndDenyAsset1
        )
        assertTrue(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset1address))
    }

    @Test
    fun `given accept known and allow Asset1 rule and target account has Asset1 but not Asset2, when transfer Asset2 from user's own account to user's target account, then signature is required`() {
        profile = profile.updateThirdPartyDepositSettings(
            account = targetAccount,
            thirdPartyDeposits = acceptKnownAndAllowAsset1
        )
        assertTrue(profile.networks[0].accounts[0].isSignatureRequiredBasedOnDepositRules(asset2address))
    }

}