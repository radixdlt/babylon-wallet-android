package rdx.works.profile.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import rdx.works.profile.data.model.serialisers.InstantSerializer
import java.time.Instant
import javax.inject.Qualifier

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ProfileSerializer

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class RelaxedSerializer

@Module
@InstallIn(SingletonComponent::class)
object SerializerModule {

    @Provides
    @ProfileSerializer
    fun provideProfileSerializer(): Json {
        return Json {
            serializersModule = SerializersModule {
                contextual(Instant::class, InstantSerializer)
            }
        }
    }

    @Provides
    @RelaxedSerializer
    fun provideRelaxedProfileSerializer(): Json {
        return Json {
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(Instant::class, InstantSerializer)
            }
        }
    }
}
