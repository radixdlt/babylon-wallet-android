package rdx.works.core.di

import okhttp3.OkHttpClient
import javax.inject.Qualifier

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class EncryptedPreferences

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class PermanentEncryptedPreferences

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class NonEncryptedPreferences

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class HostInfoPreferences

/**
 * An [OkHttpClient] that can dynamically change the base url of the network,
 * even if the [Retrofit] builder has already created the api class,
 * based on the current gateway
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DynamicGatewayHttpClient

/**
 * Same as [DynamicGatewayHttpClient] but with shorter timeout
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ShortTimeoutDynamicGatewayHttpClient

/**
 * A simple [OkHttpClient] **without** dynamic change of the base url.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class GatewayHttpClient