package rdx.works.core.sargon

import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.extensions.ProfileEntity

fun ProfileEntity.isHidden() = this.flags.contains(EntityFlag.DELETED_BY_USER)

fun ProfileEntity.isNotHidden() = this.isHidden().not()
