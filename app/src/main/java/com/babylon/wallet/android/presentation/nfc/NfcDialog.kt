package com.babylon.wallet.android.presentation.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.none
import com.babylon.wallet.android.utils.findActivity
import com.radixdlt.sargon.extensions.toBagOfBytes
import kotlinx.coroutines.launch
import rdx.works.core.toByteArray
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcDialog(
    viewModel: NfcViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    BackHandler {
        viewModel.onDismiss()
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                NfcViewModel.Event.Completed -> onDismiss()
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch { sheetState.show() }
    }

    val context = LocalContext.current
    // NFC handling state
    var isoDep = remember { mutableStateOf<IsoDep?>(null) }

    // Enable Reader Mode on start
    val activity: Activity? = remember(context) { context.findActivity() }
    val isNfcEnabledState = remember { mutableStateOf(true) }
    LaunchedEffect(activity) {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        isNfcEnabledState.value = adapter?.isEnabled == true
        if (!isNfcEnabledState.value) return@LaunchedEffect
        activity?.let { act ->
            adapter?.enableReaderMode(
                act,
                { tag: Tag? ->
                    tag?.let {
                        val dep = IsoDep.get(it)
                        dep.timeout = 60000
                        try {
                            dep.connect()
                            isoDep.value = dep
                            viewModel.onTagReady()
                        } catch (_: Throwable) {
                            isoDep.value = null
                        }
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        }
    }
    DisposableEffect(activity) {
        onDispose {
            val adapter = NfcAdapter.getDefaultAdapter(context)
            activity?.let { act -> adapter?.disableReaderMode(act) }
            try { isoDep.value?.close() } catch (_: Throwable) {}
            isoDep.value = null
        }
    }

    DefaultModalSheetLayout(
        modifier = Modifier.fillMaxSize(),
        sheetState = sheetState,
        onDismissRequest = viewModel::onDismiss,
        sheetContent = {
            Scaffold(
                topBar = {
                    RadixCenteredTopAppBar(
                        windowInsets = WindowInsets.none,
                        title = state.title,
                        onBackClick = viewModel::onDismiss,
                        backIconType = BackIconType.Close
                    )
                },
                content = { _ ->
                    if (!isNfcEnabledState.value) {
                        AlertDialog(
                            onDismissRequest = viewModel::onDismiss,
                            title = { Text(text = "NFC is turned off") },
                            text = { Text(text = "Please enable NFC in system settings to continue.") },
                            confirmButton = {
                                TextButton(onClick = viewModel::onDismiss) {
                                    Text(text = "OK")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                                    } catch (_: Throwable) {}
                                }) {
                                    Text(text = "Open settings")
                                }
                            }
                        )
                        return@Scaffold
                    }
                    // Process transceive requests against IsoDep
                    LaunchedEffect(Unit) {
                        viewModel.transceiveRequests.collect { req ->
                            val dep = isoDep.value
                            if (dep == null || !dep.isConnected) {
                                viewModel.respondException(req, IllegalStateException("Tag not connected"))
                                return@collect
                            }
                            try {
                                val response = dep.transceive(req.command.toByteArray())
                                if (response.size < 2 ||
                                    (response[response.size - 2] != 0x90.toByte() || response[response.size - 1] != 0x00.toByte())
                                ) {
                                    throw IOException("sendReceive bad status")
                                }
                                viewModel.respond(req, response.toBagOfBytes())
                            } catch (t: Throwable) {
                                viewModel.respondException(req, t)
                            }
                        }
                    }
                }
            )
        }
    )
}
