package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource

@Serializable
class Display private constructor(
    @SerialName("fiatCurrencyPriceTarget")
    val fiatCurrencyPriceTarget: String,
    @SerialName("isCurrencyAmountVisible")
    val isCurrencyAmountVisible: Boolean,
    @SerialName("ledgerHQHardwareWalletSigningDisplayMode")
    val ledgerHQHardwareWalletSigningDisplayMode: LedgerHardwareWalletFactorSource.SigningDisplayMode
) {

    override fun toString(): String {
        return "Display(" +
                "fiatCurrencyPriceTarget='$fiatCurrencyPriceTarget', " +
                "isCurrencyAmountVisible=$isCurrencyAmountVisible, " +
                "ledgerHQHardwareWalletSigningDisplayMode=$ledgerHQHardwareWalletSigningDisplayMode" +
                ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Display

        if (fiatCurrencyPriceTarget != other.fiatCurrencyPriceTarget) return false
        if (isCurrencyAmountVisible != other.isCurrencyAmountVisible) return false
        if (ledgerHQHardwareWalletSigningDisplayMode != other.ledgerHQHardwareWalletSigningDisplayMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fiatCurrencyPriceTarget.hashCode()
        result = 31 * result + isCurrencyAmountVisible.hashCode()
        result = 31 * result + ledgerHQHardwareWalletSigningDisplayMode.hashCode()
        return result
    }

    companion object {
        val default = Display(
            fiatCurrencyPriceTarget = "usd",
            isCurrencyAmountVisible = true,
            ledgerHQHardwareWalletSigningDisplayMode = LedgerHardwareWalletFactorSource.SigningDisplayMode.Verbose
        )
    }
}
