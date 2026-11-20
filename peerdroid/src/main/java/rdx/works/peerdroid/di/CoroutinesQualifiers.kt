package rdx.works.peerdroid.di

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

/**
 * The WebRTC implementation of a Radix Connect component (connector or link).
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class WebRtcTransport

/**
 * The iroh implementation of a Radix Connect component (connector or link).
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IrohTransport
