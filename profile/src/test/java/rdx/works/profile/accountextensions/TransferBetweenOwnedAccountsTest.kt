package rdx.works.profile.accountextensions

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AssetException
import com.radixdlt.sargon.AssetsExceptionList
import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DepositAddressExceptionRule
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.DepositorsAllowList
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.extensions.account
import com.radixdlt.sargon.extensions.derivePublicKey
import com.radixdlt.sargon.extensions.get
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.invoke
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import rdx.works.core.domain.DeviceInfo
import rdx.works.core.sargon.addAccounts
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.init
import rdx.works.core.sargon.initBabylon
import rdx.works.core.sargon.isSignatureRequiredBasedOnDepositRules
import rdx.works.core.sargon.updateThirdPartyDepositSettings
import rdx.works.profile.data.repository.MnemonicRepository
import kotlin.test.assertTrue

class TransferBetweenOwnedAccountsTest {

    private val mnemonicWithPassphrase = MnemonicWithPassphrase.init(
        phrase = "bright club bacon dinner achieve pull grid save ramp cereal blush woman " +
                "humble limb repeat video sudden possible story mask neutral prize goose mandate"
    )

    private val deviceInfo = DeviceInfo.sample()
    private val babylonFactorSource = FactorSource.Device.babylon(
        mnemonicWithPassphrase = mnemonicWithPassphrase,
        model = deviceInfo.model,
        name = deviceInfo.name,
        isMain = true
    )

    var profile = Profile.init(
        deviceFactorSource = babylonFactorSource,
        creatingDeviceName = deviceInfo.displayName
    )

    private val defaultNetwork = NetworkId.MAINNET

    private lateinit var targetAccount: Account

    private val asset1address = ResourceAddress.sampleMainnet.random()
    private val asset2address = ResourceAddress.sampleMainnet.random()
    private val targetAccountWithAsset1 = listOf(asset1address)

    private val acceptAll = ThirdPartyDeposits(
        depositRule = DepositRule.ACCEPT_ALL,
        assetsExceptionList = AssetsExceptionList.init(),
        depositorsAllowList = DepositorsAllowList.init()
    )

    private val acceptAllAndDenyAsset1 = ThirdPartyDeposits(
        depositRule = DepositRule.ACCEPT_ALL,
        assetsExceptionList = AssetsExceptionList.init(
            AssetException(
                address = asset1address,
                exceptionRule = DepositAddressExceptionRule.DENY
            )
        ),
        depositorsAllowList = DepositorsAllowList.init()
    )

    private val denyAll = ThirdPartyDeposits(
        depositRule = DepositRule.DENY_ALL,
        assetsExceptionList = AssetsExceptionList.init(),
        depositorsAllowList = DepositorsAllowList.init()
    )

    private val denyAllAndAllowAsset1 = ThirdPartyDeposits(
        depositRule = DepositRule.DENY_ALL,
        assetsExceptionList = AssetsExceptionList.init(
            AssetException(
                address = asset1address,
                exceptionRule = DepositAddressExceptionRule.ALLOW
            )
        ),
        depositorsAllowList = DepositorsAllowList.init()
    )

    private val denyAllAndDenyAsset1 = ThirdPartyDeposits(
        depositRule = DepositRule.DENY_ALL,
        assetsExceptionList = AssetsExceptionList.init(
            AssetException(
                address = asset1address,
                exceptionRule = DepositAddressExceptionRule.DENY
            )
        ),
        depositorsAllowList = DepositorsAllowList.init()
    )

    private val acceptKnown = ThirdPartyDeposits(
        depositRule = DepositRule.ACCEPT_KNOWN,
        assetsExceptionList = AssetsExceptionList.init(),
        depositorsAllowList = DepositorsAllowList.init()
    )

    private val acceptKnownAndAllowAsset1 = ThirdPartyDeposits(
        depositRule = DepositRule.ACCEPT_KNOWN,
        assetsExceptionList = AssetsExceptionList.init(
            AssetException(
                address = asset1address,
                exceptionRule = DepositAddressExceptionRule.ALLOW
            )
        ),
        depositorsAllowList = DepositorsAllowList.init()
    )

    private val acceptKnownAndDenyAsset1 = ThirdPartyDeposits(
        depositRule = DepositRule.ACCEPT_KNOWN,
        assetsExceptionList = AssetsExceptionList.init(
            AssetException(
                address = asset1address,
                exceptionRule = DepositAddressExceptionRule.DENY
            )
        ),
        depositorsAllowList = DepositorsAllowList.init()
    )

    @Before
    fun setUp() {
        val mnemonicRepository = mockk<MnemonicRepository>()
        coEvery { mnemonicRepository() } returns mnemonicWithPassphrase

        val derivationPath = DerivationPath.Cap26.account(
            networkId = defaultNetwork,
            keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
            index = 0u
        )
        targetAccount = Account.initBabylon(
            networkId = defaultNetwork,
            displayName = DisplayName("target account"),
            hdPublicKey = mnemonicWithPassphrase.derivePublicKey(path = derivationPath),
            factorSourceId = profile.factorSources().first().id as FactorSourceId.Hash
        )

        profile = profile.addAccounts(
            accounts = listOf(targetAccount),
            onNetwork = defaultNetwork
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