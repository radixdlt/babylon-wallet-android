package rdx.works.profile.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Display private constructor(
    @SerialName("fiatCurrencyPriceTarget")
    val fiatCurrencyPriceTarget: String
) {
    companion object {
        val default = Display(
            fiatCurrencyPriceTarget = "usd"
        )
    }
}