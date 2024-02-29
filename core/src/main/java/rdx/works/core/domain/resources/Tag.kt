package rdx.works.core.domain.resources

sealed interface Tag {
    data object Official : Tag

    data class Dynamic(
        val name: String
    ) : Tag
}
