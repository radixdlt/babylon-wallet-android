package com.babylon.wallet.android.data.gateway

import org.junit.Test

internal class GatewayErrorSerializerTest {

//    //TODO on nebunet migration network errors changed, so this response is no longer valid. We need to prepare sample error payloads that are valid and add proper tests here
//    private val TRANSACTION_NOT_FOUND_PAYLOAD = "{\"code\":404,\"message\":\"Transaction not found\",\"details\":{\"transaction_not_found\":{\"origin\":\"intent\",\"value_hex\":\"8c86978c63d9419ddd36bb9eba9d2c9c54425a59fb32237a8ab244baf1eed55b\"},\"type\":\"TransactionNotFoundError\"},\"trace_id\":\"00-fad162ed3611c68ad8e74db9cfdc8f0c-4375324cbacdae6b-00\"}"

    @Test
    fun `transaction not found properly serialized into model class`() {
    }
}
