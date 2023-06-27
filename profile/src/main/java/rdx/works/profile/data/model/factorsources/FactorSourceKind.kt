package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FactorSourceKind {
    /**
     * A user owned unencrypted mnemonic (and BIP39 passphrase) stored on device,
     * thus directly usable. This kind is used as the standard factor source for all new
     * wallet users.
     *
     * Attributes:
     * * Mine
     * * On device
     * * Hierarchical deterministic (Mnemonic)
     * * Entity creating
     */
    @SerialName("device")
    DEVICE,

    /**
     * A user owned factor source which is encrypted by security questions and stored
     * on device that supports hierarchical deterministic derivation when decrypted.
     *
     * Attributes:
     * * Mine
     * * On device
     * * Hierarchical deterministic (Mnemonic)
     * * Encrypted by Security Questions
     */
//    @SerialName("securityQuestions")
//    SECURITY_QUESTIONS,

    /**
     * A user owned hardware wallet by vendor Ledger HQ, most commonly
     * a Ledger Nano S or Ledger Nano X. Less common models are Ledger Nano S Plus
     * Ledger Stax.
     *
     * Attributes:
     * * Mine
     * * Off device
     * * Hardware (requires Browser Connector Extension to communicate with wallet)
     * * Hierarchical deterministic
     * * Entity creating (accounts only)
     */
    @SerialName("ledgerHQHardwareWallet")
    LEDGER_HQ_HARDWARE_WALLET,

    /**
     * A user known mnemonic which the user has to produce (input).
     *
     * Attributes:
     * * Mine
     * * Off Device
     * * Hierarchical deterministic (Mnemonic)
     */
    @SerialName("offDeviceMnemonic")
    OFF_DEVICE_MNEMONIC,

    /**
     * Some individual the user knows and trust, e.g. a friend or family member,
     * typically used as a factor source for the recovery role.
     *
     * Attributes:
     * * *NOT* mine
     * * Off Device
     */
    @SerialName("trustedContact")
    TRUSTED_CONTACT;
}
