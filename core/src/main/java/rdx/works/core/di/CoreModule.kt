package rdx.works.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.radixdlt.sargon.ArculusCsdkDriver
import com.radixdlt.sargon.ArculusWalletPointer
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.Bios
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.HostInteractor
import com.radixdlt.sargon.NfcTagDriver
import com.radixdlt.sargon.NfcTagDriverPurpose
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.os.driver.AndroidEventBusDriver
import com.radixdlt.sargon.os.driver.AndroidProfileStateChangeDriver
import com.radixdlt.sargon.os.driver.BiometricsHandler
import com.radixdlt.sargon.os.from
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import rdx.works.core.storage.FileRepository
import rdx.works.core.storage.FileRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CoreBindings {

    @Binds
    @Singleton
    fun bindFileRepository(
        fileRepositoryImpl: FileRepositoryImpl
    ): FileRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CoreProvider {

    @Provides
    @Singleton
    fun provideEventBusDriver(): AndroidEventBusDriver = AndroidEventBusDriver

    @Provides
    @Singleton
    fun provideProfileStateChangeDriver(): AndroidProfileStateChangeDriver = AndroidProfileStateChangeDriver

    @Suppress("LongParameterList")
    @Provides
    @Singleton
    fun provideBios(
        @ApplicationContext context: Context,
        @GatewayHttpClient httpClient: OkHttpClient,
        eventBusDriver: AndroidEventBusDriver,
        profileStateChangeDriver: AndroidProfileStateChangeDriver,
        biometricsHandler: BiometricsHandler,
        @EncryptedPreferences encryptedPreferences: DataStore<Preferences>,
        @NonEncryptedPreferences preferences: DataStore<Preferences>,
        @HostInfoPreferences hostInfoPreferences: DataStore<Preferences>,
    ): Bios = Bios.from(
        context = context,
        httpClient = httpClient,
        biometricsHandler = biometricsHandler,
        encryptedPreferencesDataStore = encryptedPreferences,
        preferencesDatastore = preferences,
        deviceInfoDatastore = hostInfoPreferences,
        eventBusDriver = eventBusDriver,
        profileStateChangeDriver = profileStateChangeDriver,
        arculusCsdkDriver = FakeArculusCsdkDriver(),
        nfcTagDriver = FakeNfcTagDriver()
    )

    @Provides
    @Singleton
    fun provideSargonOsManager(
        bios: Bios,
        hostInteractor: HostInteractor,
        @ApplicationScope applicationScope: CoroutineScope,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): SargonOsManager = SargonOsManager.factory(
        bios = bios,
        hostInteractor = hostInteractor,
        applicationScope = applicationScope,
        defaultDispatcher = defaultDispatcher
    )
}

@Suppress("TooManyFunctions")
private class FakeArculusCsdkDriver : ArculusCsdkDriver {

    override fun walletInit(): ArculusWalletPointer? {
        TODO("Not yet implemented")
    }

    override fun walletFree(wallet: ArculusWalletPointer) {
        TODO("Not yet implemented")
    }

    override fun selectWalletRequest(
        wallet: ArculusWalletPointer,
        aid: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun selectWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun createWalletSeedRequest(
        wallet: ArculusWalletPointer,
        wordCount: Long
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun createWalletSeedResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun seedPhraseFromMnemonicSentence(
        wallet: ArculusWalletPointer,
        mnemonicSentence: BagOfBytes,
        passphrase: BagOfBytes?
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun initRecoverWalletRequest(
        wallet: ArculusWalletPointer,
        wordCount: Long
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun initRecoverWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun finishRecoverWalletRequest(
        wallet: ArculusWalletPointer,
        seed: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun finishRecoverWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun resetWalletRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun resetWalletResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun getGguidRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun getGguidResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun getFirmwareVersionRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun getFirmwareVersionResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun storeDataPinRequest(
        wallet: ArculusWalletPointer,
        pin: String
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun storeDataPinResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun verifyPinRequest(
        wallet: ArculusWalletPointer,
        pin: String
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun verifyPinResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun initEncryptedSessionRequest(wallet: ArculusWalletPointer): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun initEncryptedSessionResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): Int {
        TODO("Not yet implemented")
    }

    override fun getPublicKeyByPathRequest(
        wallet: ArculusWalletPointer,
        path: BagOfBytes,
        curve: UShort
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun getPublicKeyByPathResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }

    override fun signHashPathRequest(
        wallet: ArculusWalletPointer,
        path: BagOfBytes,
        curve: UShort,
        algorithm: UByte,
        hash: BagOfBytes
    ): List<BagOfBytes>? {
        TODO("Not yet implemented")
    }

    override fun signHashPathResponse(
        wallet: ArculusWalletPointer,
        response: BagOfBytes
    ): BagOfBytes? {
        TODO("Not yet implemented")
    }
}

private class FakeNfcTagDriver : NfcTagDriver {

    override suspend fun startSession(purpose: NfcTagDriverPurpose) {
        TODO("Not yet implemented")
    }

    override suspend fun endSession(withFailure: CommonException?) {
        TODO("Not yet implemented")
    }

    override suspend fun sendReceive(command: BagOfBytes): BagOfBytes {
        TODO("Not yet implemented")
    }

    override suspend fun sendReceiveCommandChain(commands: List<BagOfBytes>): BagOfBytes {
        TODO("Not yet implemented")
    }

    override suspend fun setMessage(message: String) {
        TODO("Not yet implemented")
    }
}
