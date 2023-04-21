package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Display private constructor(
    @SerialName("fiatCurrencyPriceTarget")
    val fiatCurrencyPriceTarget: String,
    @SerialName("isCurrencyAmountVisible")
    val isCurrencyAmountVisible: Boolean
) {
    companion object {
        val default = Display(
            fiatCurrencyPriceTarget = "usd",
            isCurrencyAmountVisible = true
        )
    }
}
