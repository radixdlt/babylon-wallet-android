package rdx.works.core.di

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
annotation class NonEncryptedPreferences

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DeviceInfoPreferences