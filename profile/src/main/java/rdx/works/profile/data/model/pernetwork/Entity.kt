package rdx.works.profile.data.model.pernetwork

sealed interface Entity {
    val networkID: Int
    val address: String
    val securityState: SecurityState
}
