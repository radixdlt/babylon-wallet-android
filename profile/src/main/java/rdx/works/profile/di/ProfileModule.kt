package rdx.works.profile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rdx.works.profile.Crypto
import rdx.works.profile.CryptoImpl
import rdx.works.profile.enginetoolkit.EngineToolkit
import rdx.works.profile.enginetoolkit.EngineToolkitImpl

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

}
