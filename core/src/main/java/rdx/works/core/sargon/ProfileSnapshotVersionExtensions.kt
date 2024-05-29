package rdx.works.core.sargon

import com.radixdlt.sargon.ProfileSnapshotVersion

fun ProfileSnapshotVersion.Companion.fromVersion(value: UShort) = ProfileSnapshotVersion.entries.find {
    it.value == value
} ?: error("No snapshot version with $value exists")
