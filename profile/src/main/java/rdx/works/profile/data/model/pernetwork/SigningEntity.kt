package rdx.works.profile.data.model.pernetwork

sealed interface SigningEntity {
    val networkID: Int
    val address: String
    val securityState: SecurityState
}
