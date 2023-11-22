package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FactorSourceFlag {
    // Used to mark a "babylon" `.device` FactorSource as "main". All new accounts
    // and Personas are created using the `main` `DeviceFactorSource`.
    //
    // We can only ever have one.
    // We might have zero `main` flags across all  `DeviceFactorSource`s if and only if we have only one  `DeviceFactorSource`s.
    // If we have two or more  `DeviceFactorSource`s one of them MUST
    // be marked with `main`.
    @SerialName("main")
    Main,

    @SerialName("deletedByUser")
    DeletedByUser
}
