package rdx.works.core.domain.resources.metadata

enum class AccountType(val asString: String) {
    DAPP_DEFINITION("dapp definition");

    companion object {
        fun from(value: String) = entries.find { it.asString == value }
    }
}
