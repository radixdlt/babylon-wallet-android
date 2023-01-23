package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.AccountDto
import com.babylon.wallet.android.data.dapp.model.AccountWithProofOfOwnership
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OneTimePersonaDataRequestItem
import com.babylon.wallet.android.data.dapp.model.OngoingAccountsRequestItem
import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.data.dapp.model.WalletUnauthorizedRequestItems
import com.babylon.wallet.android.data.dapp.model.walletRequestJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Test

class WalletInteractionModelsTest {

    @Test
    fun `OneTimeAccountsRequestResponseItem serialization & deserialization`() {
        val responseItem1: OneTimeAccountsRequestResponseItem = OneTimeAccountsWithProofOfOwnershipRequestResponseItem(
            listOf(
                AccountWithProofOfOwnership(
                    accountDto = AccountDto(address = "address2", label = "Test account", appearanceId = 0),
                    challenge = "1",
                    signature = "2"
                ), AccountWithProofOfOwnership(
                    accountDto = AccountDto(address = "address2", label = "Test account", appearanceId = 0),
                    challenge = "1",
                    signature = "2"
                )
            )
        )
        val responseItem2: OneTimeAccountsRequestResponseItem =
            OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem(
                listOf(
                    AccountDto(address = "address1", label = "Test account", appearanceId = 0),
                    AccountDto(address = "address2", label = "Test account", appearanceId = 1)
                )
            )

        val string1 = walletRequestJson.encodeToString(responseItem1)
        val string2 = walletRequestJson.encodeToString(responseItem2)
        assert(string1.isNotEmpty())
        assert(string2.isNotEmpty())
    }

    @Test
    fun `WalletInteraction unauthorized request decoding with oneTimeAccounts item`() {
        val request = """
            {
               "items":{
                  "discriminator":"unauthorized",
                  "oneTimeAccounts":{
                     "requiresProofOfOwnership":false,
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppId":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletUnauthorizedRequestItems)
        val item = result.items as WalletUnauthorizedRequestItems
        assert(item.oneTimeAccounts?.requiresProofOfOwnership == false)
    }

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

        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        val item = result.items

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

        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        val item = result.items


        val numberOfAccounts = (item as OngoingAccountsRequestItem).numberOfAccounts
        assert(numberOfAccounts.quantity == 1)
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

        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        val item = result.items

        val fields = (item as OneTimePersonaDataRequestItem).fields
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

        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        val item = result.items

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

        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        val item = result.items

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

        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        val item = result.items

    }
}
