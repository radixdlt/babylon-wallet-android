package rdx.works.core.domain

private const val LITERAL_LIGHT = "light"
private const val LITERAL_DARK = "dark"
private const val LITERAL_SYSTEM = "system"

enum class ThemeSelection(
    private val literal: String
) {
    LIGHT(literal = LITERAL_LIGHT),
    DARK(literal = LITERAL_DARK),
    SYSTEM(literal = LITERAL_SYSTEM);

    internal fun toLiteral() = literal

    companion object {
        val DEFAULT = SYSTEM

        internal fun fromLiteral(literal: String) = when (literal) {
            LITERAL_LIGHT -> LIGHT
            LITERAL_DARK -> DARK
            LITERAL_SYSTEM -> SYSTEM
            else -> error("No enum variant associated to `$literal` could be found for ThemeSelection.")
        }
    }
}
