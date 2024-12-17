package rdx.works.core.sargon

import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.extensions.ProfileEntity

fun ProfileEntity.isHidden() = this.flags.contains(EntityFlag.HIDDEN_BY_USER)

fun ProfileEntity.isNotHidden() = this.isHidden().not()

fun ProfileEntity.isDeleted() = this.flags.contains(EntityFlag.TOMBSTONED_BY_USER)
