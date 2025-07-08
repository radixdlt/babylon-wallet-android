package com.babylon.wallet.android.presentation.addfactorsource.name

import android.net.Uri
import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.addfactorsource.ROUTE_ADD_FACTOR_SOURCE_GRAPH
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletModel
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson

private const val DESTINATION_SET_FACTOR_NAME = "set_factor_name"
private const val ARG_FACTOR_SOURCE_KIND = "arg_factor_source_kind"
private const val ARG_MWP = "arg_mwp"
private const val ARG_FSID = "arg_fsid"
private const val ARG_LEDGER_MODEL = "arg_ledger_model"

const val ROUTE_SET_FACTOR_NAME =
    "$DESTINATION_SET_FACTOR_NAME?$ARG_FACTOR_SOURCE_KIND={$ARG_FACTOR_SOURCE_KIND}&$ARG_MWP={$ARG_MWP}"

internal sealed class SetFactorNameArgs(
    open val factorSourceKind: FactorSourceKind
) {

    data class WithMnemonic(
        override val factorSourceKind: FactorSourceKind,
        val mnemonicWithPassphrase: MnemonicWithPassphrase
    ) : SetFactorNameArgs(
        factorSourceKind = factorSourceKind
    )

    data class ForLedger(
        val factorSourceId: FactorSourceId.Hash,
        val ledgerModel: LedgerHardwareWalletModel
    ) : SetFactorNameArgs(
        factorSourceKind = factorSourceId.value.kind
    )

    companion object {

        fun from(savedStateHandle: SavedStateHandle): SetFactorNameArgs {
            val factorSourceId = savedStateHandle.get<String>(ARG_FSID)?.let {
                FactorSourceId.Hash.fromJson(it)
            }

            return if (factorSourceId != null) {
                val ledgerModel = checkNotNull(savedStateHandle.get<LedgerHardwareWalletModel>(ARG_LEDGER_MODEL))

                ForLedger(
                    factorSourceId = factorSourceId,
                    ledgerModel = ledgerModel
                )
            } else {
                val factorSourceKind = checkNotNull(savedStateHandle.get<FactorSourceKind>(ARG_FACTOR_SOURCE_KIND))
                val mnemonicWithPassphrase = MnemonicWithPassphrase.fromJson(checkNotNull(savedStateHandle[ARG_MWP]))

                WithMnemonic(
                    factorSourceKind = factorSourceKind,
                    mnemonicWithPassphrase = mnemonicWithPassphrase
                )
            }
        }
    }
}

fun NavController.setFactorName(
    factorSourceKind: FactorSourceKind,
    mnemonicWithPassphrase: MnemonicWithPassphrase
) {
    val mwpArg = Uri.encode(mnemonicWithPassphrase.toJson())
    navigate("$DESTINATION_SET_FACTOR_NAME?$ARG_FACTOR_SOURCE_KIND=$factorSourceKind&$ARG_MWP=$mwpArg")
}

fun NavController.setFactorName(
    factorSourceId: FactorSourceId.Hash,
    ledgerModel: LedgerHardwareWalletModel
) {
    val fsidArg = Uri.encode(factorSourceId.toJson())
    navigate("$DESTINATION_SET_FACTOR_NAME?$ARG_FSID=$fsidArg&$ARG_LEDGER_MODEL=$ledgerModel")
}

fun NavGraphBuilder.setFactorName(
    navController: NavController
) {
    composable(
        route = ROUTE_SET_FACTOR_NAME,
        arguments = listOf(
            navArgument(ARG_FACTOR_SOURCE_KIND) {
                type = NavType.EnumType(FactorSourceKind::class.java)
                nullable = true
            },
            navArgument(ARG_MWP) {
                type = MnemonicWithPassphraseParamType()
                nullable = true
            },
            navArgument(ARG_FSID) {
                type = FactorSourceIdHashParamType()
                nullable = true
            },
        ),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SetFactorSourceNameScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onSaved = { navController.popBackStack(ROUTE_ADD_FACTOR_SOURCE_GRAPH, true) }
        )
    }
}

private class MnemonicWithPassphraseParamType : NavType<MnemonicWithPassphrase>(
    isNullableAllowed = true
) {

    override fun get(bundle: Bundle, key: String): MnemonicWithPassphrase = MnemonicWithPassphrase.fromJson(
        requireNotNull(bundle.getString(key))
    )

    override fun parseValue(value: String): MnemonicWithPassphrase {
        return MnemonicWithPassphrase.fromJson(value)
    }

    override fun put(bundle: Bundle, key: String, value: MnemonicWithPassphrase) {
        bundle.putString(key, value.toJson())
    }
}

private class FactorSourceIdHashParamType : NavType<FactorSourceId.Hash>(
    isNullableAllowed = true
) {

    override fun get(bundle: Bundle, key: String): FactorSourceId.Hash = FactorSourceId.Hash.fromJson(
        requireNotNull(bundle.getString(key))
    )

    override fun parseValue(value: String): FactorSourceId.Hash {
        return FactorSourceId.Hash.fromJson(value)
    }

    override fun put(bundle: Bundle, key: String, value: FactorSourceId.Hash) {
        bundle.putString(key, value.toJson())
    }
}
