package com.babylon.wallet.android.data.gateway.generated.infrastructure

import com.babylon.wallet.android.data.gateway.serialisers.AnyAsJsonElementSerializer
import com.radixdlt.sargon.Decimal192
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import rdx.works.core.domain.Serializer
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object Serializer {

    const val MIME_TYPE = "application/json"

    @JvmStatic
    val kotlinxSerializationAdapters = SerializersModule {
        contextual(BigDecimal::class, BigDecimalAdapter)
        contextual(BigInteger::class, BigIntegerAdapter)
        contextual(Decimal192::class, Decimal192.Serializer)
        contextual(LocalDate::class, LocalDateAdapter)
        contextual(LocalDateTime::class, LocalDateTimeAdapter)
        contextual(OffsetDateTime::class, OffsetDateTimeAdapter)
        contextual(UUID::class, UUIDAdapter)
        contextual(AtomicInteger::class, AtomicIntegerAdapter)
        contextual(AtomicLong::class, AtomicLongAdapter)
        contextual(AtomicBoolean::class, AtomicBooleanAdapter)
        contextual(URI::class, URIAdapter)
        contextual(URL::class, URLAdapter)
        contextual(StringBuilder::class, StringBuilderAdapter)
        contextual(Any::class, AnyAsJsonElementSerializer)
    }

    @JvmStatic
    val kotlinxSerializationJson: Json by lazy {
        Json {
            serializersModule = kotlinxSerializationAdapters
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}
