package rdx.works.profile.data

import android.content.Context
import android.content.SharedPreferences
import com.radixdlt.sargon.Timestamp
import com.radixdlt.sargon.Uuid
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import rdx.works.core.serializers.TimestampSerializer
import rdx.works.core.serializers.UuidSerializer
import rdx.works.profile.data.repository.HostInfoRepositoryImpl
import kotlin.test.assertEquals

class HostInfoRepositoryTest {

    private val context = mockk<Context>()
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()
    private val sut = HostInfoRepositoryImpl(context)

    init {
        every { context.getSharedPreferences(HostInfoRepositoryImpl.PREFS, Context.MODE_PRIVATE) } returns preferences
        every { preferences.edit() } returns editor
        every { editor.apply() } just runs
        every { editor.commit() } returns true
        every { editor.putString(any(), any()) } returns editor
    }

    @Test
    fun `given no host id stored, when get host id is invoked, then a new id and date are generated`() {
        every { preferences.getString(HostInfoRepositoryImpl.HOST_ID, null) } returns null

        val hostId = sut.getHostId()

        val entryToVerify = NewEntry(hostId.id, hostId.generatedAt)
        verify { editor.putString(HostInfoRepositoryImpl.HOST_ID, Json.encodeToString(entryToVerify)) }
    }

    @Test
    fun `given host id stored with the new entry data, when get host id is invoked, then the existing id is returned`() {
        val savedEntry = NewEntry(id = Uuid.randomUUID(), date = Timestamp.now())
        every { preferences.getString(HostInfoRepositoryImpl.HOST_ID, null) } returns Json.encodeToString(savedEntry)

        val hostId = sut.getHostId()

        assertEquals(
            savedEntry.id,
            hostId.id
        )
        assertEquals(
            savedEntry.date,
            hostId.generatedAt
        )
        // Also verify that there was no need to save anything back to preferences
        verify(exactly = 0) { editor.putString(any(), any()) }
    }

    @Test
    fun `given host id stored with the old entry data, when get host id is invoked, then the existing id is returned`() {
        val savedEntry = OldEntry(
            id = Uuid.randomUUID(),
            date = Timestamp.now(),
            name = "My Pixel",
            manufacturer = "Google",
            model = "Pixel 8 Pro"
        )
        every { preferences.getString(HostInfoRepositoryImpl.HOST_ID, null) } returns Json.encodeToString(savedEntry)

        val hostId = sut.getHostId()

        assertEquals(
            savedEntry.id,
            hostId.id
        )
        assertEquals(
            savedEntry.date,
            hostId.generatedAt
        )
        // Also verify that there was no need to save anything back to preferences
        verify(exactly = 0) { editor.putString(any(), any()) }
    }

    // The new data type stored into preferences
    @Serializable
    private data class NewEntry(
        @Serializable(with = UuidSerializer::class)
        val id: Uuid,
        @Serializable(with = TimestampSerializer::class)
        val date: Timestamp
    )

    // The old data type stored into preferences
    @Serializable
    private data class OldEntry(
        @Serializable(with = UuidSerializer::class)
        val id: Uuid,
        @Serializable(with = TimestampSerializer::class)
        val date: Timestamp,
        val name: String,
        val manufacturer: String,
        val model: String
    )
}