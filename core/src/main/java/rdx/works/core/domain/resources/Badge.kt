package rdx.works.core.domain.resources

data class Badge(
    val resource: Resource
) {

    val name: String?
        get() = resource.name.takeIf { it.isNotBlank() }
}
