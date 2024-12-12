package com.babylon.wallet.android.data.repository.securityshield

import com.radixdlt.sargon.SecurityShieldBuilder
import javax.inject.Singleton

@Singleton
class SecurityShieldBuilderClient {

    private lateinit var securityShieldBuilder: SecurityShieldBuilder

    fun newBuilder() {
        securityShieldBuilder = SecurityShieldBuilder()
    }
}