package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.AccountDto
import com.babylon.wallet.android.data.dapp.model.AccountWithProofOfOwnership
import com.babylon.wallet.android.data.dapp.model.AuthLoginRequestItem
import com.babylon.wallet.android.data.dapp.model.AuthUsePersonaRequestItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.SendTransactionResponseItem
import com.babylon.wallet.android.data.dapp.model.WalletAuthorizedRequestItems
import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletTransactionItems
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletUnauthorizedRequestItems
import com.babylon.wallet.android.data.dapp.model.walletRequestJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert
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
                  "discriminator":"unauthorizedRequest",
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
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletUnauthorizedRequestItems)
        val item = result.items as WalletUnauthorizedRequestItems
        assert(item.oneTimeAccounts?.requiresProofOfOwnership == false)
    }

    @Test
    fun `WalletInteraction unauthorized request decoding with oneTimePersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"unauthorizedRequest",
                  "oneTimePersonaData":{
                     "fields":["name", "address"]
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletUnauthorizedRequestItems)
        val item = result.items as WalletUnauthorizedRequestItems
        assert(item.oneTimePersonaData?.fields?.size == 2)
    }

    @Test
    fun `WalletInteraction authorized usePersona request decoding with oneTimeAccounts item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"usePersona",
                    "identityAddress":"randomAddress1"
                  }
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
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.auth is AuthUsePersonaRequestItem)
        assert(item.oneTimeAccounts?.requiresProofOfOwnership == false)
    }

    @Test
    fun `WalletInteraction authorized usePersona request decoding with ongoingAccounts item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"usePersona",
                    "identityAddress":"randomAddress1"
                  }
                  "ongoingAccounts":{
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
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.auth is AuthUsePersonaRequestItem)
        assert(item.ongoingAccounts?.requiresProofOfOwnership == false)
    }

    @Test
    fun `WalletInteraction authorized usePersona request decoding with oneTimePersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"usePersona",
                    "identityAddress":"randomAddress1"
                  }
                  "oneTimePersonaData":{
                     "fields":["name", "address"]
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.oneTimePersonaData?.fields?.size == 2)
    }

    @Test
    fun `WalletInteraction authorized usePersona request decoding with ongoingPersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"usePersona",
                    "identityAddress":"randomAddress1"
                  }
                  "ongoingPersonaData":{
                     "fields":["name", "address"]
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.ongoingPersonaData?.fields?.size == 2)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with oneTimeAccounts item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"login"                   
                  }
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
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.auth is AuthLoginRequestItem)
        assert(item.oneTimeAccounts?.requiresProofOfOwnership == false)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with ongoingAccounts item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"login",
                    "challenge":"randomChallenge"
                  }
                  "ongoingAccounts":{
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
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.auth is AuthLoginRequestItem)
        assert(item.ongoingAccounts?.requiresProofOfOwnership == false)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with oneTimePersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"login"                   
                  }
                  "oneTimePersonaData":{
                     "fields":["name", "address"]
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.auth is AuthLoginRequestItem)
        assert(item.oneTimePersonaData?.fields?.size == 2)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with ongoingPersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"login",
                    "challenge":"randomChallenge"
                  }
                  "ongoingPersonaData":{
                     "fields":["name", "address"]
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.auth is AuthLoginRequestItem)
        assert(item.ongoingPersonaData?.fields?.size == 2)
    }

    @Test
    fun `WalletInteraction request decoding with transaction item`() {
        val request = """
            {
               "items":{
                  "discriminator":"transaction",
                  "send":{
                      "transactionManifest":"manifest"   
                      "version":1,
                      "blobs":["blob1", "blob2"],
                      "message":"manifest"
                  }                                  
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = walletRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletTransactionItems)
        val item = result.items as WalletTransactionItems
        assert(item.send.transactionManifest == "manifest")
    }

    @Test
    fun `transaction approval response matches expected`() {
        val expected = """{"discriminator":"success","interactionId":"1","items":{"discriminator":"transaction","send":{"transactionIntentHash":"1"}}}"""
        val response: WalletInteractionResponse = WalletInteractionSuccessResponse(
            interactionId = "1", items = WalletTransactionResponseItems(SendTransactionResponseItem("1"))
        )
        val result = walletRequestJson.encodeToString(response)
        Assert.assertEquals(expected, result)
    }

}
