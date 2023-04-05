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
    @SerialName("securityQuestions")
    SECURITY_QUESTIONS,

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
     */
    @SerialName("ledgerHQHardwareWallet")
    LEDGER_HQ_HARDWARE_WALLET,

    /**
     * A user owned hardware wallet by vendor YubiCo, which the user has to produce (connect)
     * in order to use. Most common model might be YubiKey 5C NFC.
     *
     * Attributes:
     * * Mine
     * * Off device
     * * Hardware (directly readable by wallet)
     */
    @SerialName("yubiKey")
    YUBIKEY,

    /**
     * A user known single key which the user has to produce (input).
     *
     * Attributes:
     * * Mine
     * * Off Device
     */
    @SerialName("offDeviceSingleKey")
    OFF_DEVICE_SINGLE_KEY,

    /**
     * A user known mnemonic which the user has to produce (input).
     *
     * Attributes:
     * * Mine
     * * Off Device
     * * Hierarchical deterministic
     */
    @SerialName("offDeviceMnemonic")
    OFF_DEVICE_MNEMONIC,

    /**
     * A user known secret acting as input key material (*IKM*) for some
     * function which maps it to Entropy for a Mnemonic.
     *
     * Attributes:
     * * Mine
     * * Off Device
     * * Hierarchical deterministic
     */
    @SerialName("offDeviceInputKeyMaterialForMnemonic")
    OFF_DEVICE_INPUT_KEY_MATERIAL_FOR_MNEMONIC,

    /**
     * Some individual the user knows and trust, e.g. a friend or family member,
     * typically used as a factor source for the recovery role.
     *
     * Attributes:
     * * *NOT* mine
     * * Off Device
     */
    @SerialName("trustedContact")
    TRUSTED_CONTACT,

    /**
     * Some entity the user knows and trust, e.g. a company,
     * typically used as a factor source for the recovery role.
     *
     * Attributes:
     * * *Not* mine
     * * Off Device
     */
    @SerialName("trustedEnterprise")
    TRUSTED_ENTERPRISE;

    val isHierarchicalDeterministic: Boolean
        get() = when (this) {
            DEVICE,
            LEDGER_HQ_HARDWARE_WALLET,
            OFF_DEVICE_MNEMONIC,
            SECURITY_QUESTIONS,
            OFF_DEVICE_INPUT_KEY_MATERIAL_FOR_MNEMONIC -> true
            YUBIKEY,
            OFF_DEVICE_SINGLE_KEY,
            TRUSTED_CONTACT,
            TRUSTED_ENTERPRISE -> false
        }

    val isOnDevice: Boolean
        get() = when (this) {
            DEVICE,
            SECURITY_QUESTIONS -> true
            LEDGER_HQ_HARDWARE_WALLET,
            YUBIKEY,
            OFF_DEVICE_SINGLE_KEY,
            OFF_DEVICE_MNEMONIC,
            OFF_DEVICE_INPUT_KEY_MATERIAL_FOR_MNEMONIC,
            TRUSTED_CONTACT,
            TRUSTED_ENTERPRISE -> false
        }

    val isMine: Boolean
        get() = when (this) {
            DEVICE,
            SECURITY_QUESTIONS,
            LEDGER_HQ_HARDWARE_WALLET,
            YUBIKEY,
            OFF_DEVICE_SINGLE_KEY,
            OFF_DEVICE_MNEMONIC,
            OFF_DEVICE_INPUT_KEY_MATERIAL_FOR_MNEMONIC -> true
            TRUSTED_CONTACT,
            TRUSTED_ENTERPRISE -> false
        }

    /**
     * Returns the kind of a hardware a factor source uses. If the factor source is not
     * of hardware type, then it returns null.
     */
    val hardwareKind: HardwareKind?
        get() = when (this) {
            LEDGER_HQ_HARDWARE_WALLET -> HardwareKind.REQUIRES_BROWSER_CONNECTOR_EXTENSION
            YUBIKEY -> HardwareKind.DIRECT
            DEVICE,
            SECURITY_QUESTIONS,
            OFF_DEVICE_SINGLE_KEY,
            OFF_DEVICE_MNEMONIC,
            OFF_DEVICE_INPUT_KEY_MATERIAL_FOR_MNEMONIC,
            TRUSTED_CONTACT,
            TRUSTED_ENTERPRISE -> null
        }

    enum class HardwareKind {
        /**
         * A hardware that can directly communicate with the wallet
         */
        DIRECT,

        /**
         * A hardware that requires a browser with the connector extension
         * in order to connect
         */
        REQUIRES_BROWSER_CONNECTOR_EXTENSION
    }
}
