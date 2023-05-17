package rdx.works.profile.data.model

import rdx.works.profile.data.model.pernetwork.SecurityState

interface SigningEntity {
    val networkID: Int
    val address: String
    val securityState: SecurityState
}
