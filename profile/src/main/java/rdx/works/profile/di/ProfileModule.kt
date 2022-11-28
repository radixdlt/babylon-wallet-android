package rdx.works.profile.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rdx.works.profile.data.crypto.Crypto
import rdx.works.profile.data.crypto.CryptoImpl
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.ProfileRepositoryImpl
import rdx.works.profile.domain.GetMnemonicUseCase
import rdx.works.profile.domain.GenerateProfileUseCase
import rdx.works.profile.enginetoolkit.EngineToolkit
import rdx.works.profile.enginetoolkit.EngineToolkitImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ProfileModule {

    @Binds
    fun bindCrypto(
        cryptoImpl: CryptoImpl
    ): Crypto

    @Binds
    fun bindEngineToolkit(
        engineToolkitImpl: EngineToolkitImpl
    ): EngineToolkit

    @Binds
    fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository

}
