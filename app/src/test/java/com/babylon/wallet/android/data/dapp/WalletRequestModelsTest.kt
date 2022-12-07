package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.*
import kotlinx.serialization.decodeFromString
import org.junit.Test

class WalletRequestModelsTest {

    @Test
    fun `given a oneTimeAccountsRead wallet request when decoding then result is a OneTimeAccountsReadRequestItem object`() {
        val request = """
            {
              "items": [
                {
                  "requestType": "oneTimeAccountsRead",
                  "requiresProofOfOwnership": false
                }
              ],
              "requestId": "4abe2cb1-93e2-467d-a854-5e2cec897c50",
              "metadata": {
                "networkId": 34,
                "origin": "https://dashboard-hammunet.rdx-works-main.extratools.works",
                "dAppId": "dashboard"
              }
            }
        """

        val result = walletRequestJson.decodeFromString<WalletRequest>(request)
        val item = result.items[0]

        assert(item is OneTimeAccountsReadRequestItem)
    }

    @Test
    fun `given a ongoingAccountsRead wallet request with one number of accounts when decoding then result is a OngoingAccountsReadRequestItem object`() {
        val request = """
            {
              "items": [
                {
                  "requestType": "ongoingAccountsRead",
                  "requiresProofOfOwnership": true,
                  "numberOfAccounts": 1
                }
              ],
              "requestId": "4abe2cb1-93e2-467d-a854-5e2cec897c50",
              "metadata": {
                "networkId": 34,
                "origin": "https://dashboard-hammunet.rdx-works-main.extratools.works",
                "dAppId": "dashboard"
              }
            }
        """

        val result = walletRequestJson.decodeFromString<WalletRequest>(request)
        val item = result.items[0]

        assert(item is OngoingAccountsReadRequestItem)

        val numberOfAccounts = (item as OngoingAccountsReadRequestItem).numberOfAccounts
        assert(numberOfAccounts == 1)
    }

    @Test
    fun `given a oneTimePersonaDataRead wallet request when decoding then result is a OneTimePersonaDataReadRequestItem object`() {
        val request = """
            {
              "items": [
                {
                  "requestType": "oneTimePersonaDataRead",
                  "fields": ["firstName", "lastName"]
                }
              ],
              "requestId": "4abe2cb1-93e2-467d-a854-5e2cec897c50",
              "metadata": {
                "networkId": 34,
                "origin": "https://dashboard-hammunet.rdx-works-main.extratools.works",
                "dAppId": "dashboard"
              }
            }
        """

        val result = walletRequestJson.decodeFromString<WalletRequest>(request)
        val item = result.items[0]

        assert(item is OneTimePersonaDataReadRequestItem)

        val fields = (item as OneTimePersonaDataReadRequestItem).fields
        assert(fields.size == 2)
    }

    @Test
    fun `given a ongoingPersonaDataRead wallet request when decoding then result is a OngoingPersonaDataReadRequestItem object`() {
        val request = """
            {
              "items": [
                {
                  "requestType": "ongoingPersonaDataRead",
                  "fields": []
                }
              ],
              "requestId": "4abe2cb1-93e2-467d-a854-5e2cec897c50",
              "metadata": {
                "networkId": 34,
                "origin": "https://dashboard-hammunet.rdx-works-main.extratools.works",
                "dAppId": "dashboard"
              }
            }
        """

        val result = walletRequestJson.decodeFromString<WalletRequest>(request)
        val item = result.items[0]

        assert(item is OngoingPersonaDataReadRequestItem)
    }

    @Test
    fun `given a usePersonaRead wallet request when decoding then result is a UsePersonaReadRequestItem object`() {
        val request = """
            {
              "items": [
                {
                  "requestType": "usePersonaRead",
                  "id": "aCoolId"
                }
              ],
              "requestId": "4abe2cb1-93e2-467d-a854-5e2cec897c50",
              "metadata": {
                "networkId": 34,
                "origin": "https://dashboard-hammunet.rdx-works-main.extratools.works",
                "dAppId": "dashboard"
              }
            }
        """

        val result = walletRequestJson.decodeFromString<WalletRequest>(request)
        val item = result.items[0]

        assert(item is UsePersonaReadRequestItem)
    }

    @Test
    fun `given a loginRead wallet request when decoding then result is a LoginReadRequestItem object`() {
        val request = """
            {
              "items": [
                {
                  "requestType": "loginRead",
                  "challenge": "aChallengeForFun"
                }
              ],
              "requestId": "4abe2cb1-93e2-467d-a854-5e2cec897c50",
              "metadata": {
                "networkId": 34,
                "origin": "https://dashboard-hammunet.rdx-works-main.extratools.works",
                "dAppId": "dashboard"
              }
            }
        """

        val result = walletRequestJson.decodeFromString<WalletRequest>(request)
        val item = result.items[0]

        assert(item is LoginReadRequestItem)
    }
}
