package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.AccountDto
import com.babylon.wallet.android.data.dapp.model.AccountWithProofOfOwnership
import com.babylon.wallet.android.data.dapp.model.AuthLoginWithChallengeRequestItem
import com.babylon.wallet.android.data.dapp.model.AuthLoginWithoutChallengeRequestItem
import com.babylon.wallet.android.data.dapp.model.AuthUsePersonaRequestItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithProofOfOwnershipRequestItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithoutProofOfOwnershipRequestItem
import com.babylon.wallet.android.data.dapp.model.OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.OngoingAccountsWithProofOfOwnershipRequestItem
import com.babylon.wallet.android.data.dapp.model.OngoingAccountsWithoutProofOfOwnershipRequestItem
import com.babylon.wallet.android.data.dapp.model.ProofDto
import com.babylon.wallet.android.data.dapp.model.SendTransactionResponseItem
import com.babylon.wallet.android.data.dapp.model.WalletAuthorizedRequestItems
import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletTransactionItems
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletUnauthorizedRequestItems
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert
import org.junit.Test

class WalletInteractionModelsTest {

    @Test
    fun `OneTimeAccountsRequestResponseItem serialization & deserialization`() {
        val responseItem1: OneTimeAccountsRequestResponseItem = OneTimeAccountsWithProofOfOwnershipRequestResponseItem(
            challenge = "challenge",
            accounts = listOf(
                AccountWithProofOfOwnership(
                    accountDto = AccountDto(
                        address = "address2",
                        label = "Test account",
                        appearanceId = 0
                    ),
                    proof = ProofDto(
                        publicKey = "publicKey",
                        signature = "signature",
                        curve = "curve"
                    )
                ), AccountWithProofOfOwnership(
                    accountDto = AccountDto(
                        address = "address2",
                        label = "Test account",
                        appearanceId = 0
                    ),
                    proof = ProofDto(
                        publicKey = "publicKey",
                        signature = "signature",
                        curve = "curve"
                    )
                )
            )
        )
        val responseItem2: OneTimeAccountsRequestResponseItem =
            OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem(
                listOf(
                    AccountDto(
                        address = "address1",
                        label = "Test account",
                        appearanceId = 0
                    ),
                    AccountDto(
                        address = "address2",
                        label = "Test account",
                        appearanceId = 1
                    )
                )
            )

        val string1 = peerdroidRequestJson.encodeToString(responseItem1)
        val string2 = peerdroidRequestJson.encodeToString(responseItem2)
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
                     "discriminator": "oneTimeAccountsWithoutProofOfOwnership",
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletUnauthorizedRequestItems)
        val item = result.items as WalletUnauthorizedRequestItems
        assert(item.oneTimeAccounts is OneTimeAccountsWithoutProofOfOwnershipRequestItem)
    }

    @Test
    fun `WalletInteraction unauthorized request decoding with oneTimePersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"unauthorizedRequest",
                  "oneTimePersonaData":{
                    "fields":["givenName", "emailAddress"]
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
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
                     "discriminator":"oneTimeAccountsWithoutProofOfOwnership",
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.auth is AuthUsePersonaRequestItem)
        assert(item.oneTimeAccounts is OneTimeAccountsWithoutProofOfOwnershipRequestItem)
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
                     "discriminator":"ongoingAccountsWithoutProofOfOwnership",
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.auth is AuthUsePersonaRequestItem)
        assert(item.ongoingAccounts is OngoingAccountsWithoutProofOfOwnershipRequestItem)
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
                    "fields":["givenName", "emailAddress"]
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
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
                     "fields":["givenName", "emailAddress"]
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
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
                    "discriminator":"loginWithoutChallenge"                   
                  }
                  "oneTimeAccounts":{
                     "discriminator": "oneTimeAccountsWithProofOfOwnership",
                     "challenge": "challenge",
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assertEquals(AuthLoginWithoutChallengeRequestItem, item.auth)
        assert(item.oneTimeAccounts is OneTimeAccountsWithProofOfOwnershipRequestItem)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with ongoingAccounts item without proof of ownership`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"loginWithChallenge",
                    "challenge":"randomChallenge"
                  }
                  "ongoingAccounts":{
                     "discriminator":"ongoingAccountsWithoutProofOfOwnership",
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assertEquals(AuthLoginWithChallengeRequestItem("randomChallenge"), item.auth)
        assert(item.ongoingAccounts is OngoingAccountsWithoutProofOfOwnershipRequestItem)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with ongoingAccounts item with proof of ownership`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"loginWithChallenge",
                    "challenge":"randomChallenge"
                  }
                  "ongoingAccounts":{
                     "discriminator":"ongoingAccountsWithProofOfOwnership",
                     "challenge":"challenge"
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assertEquals(AuthLoginWithChallengeRequestItem("randomChallenge"), item.auth)
        assert(item.ongoingAccounts is OngoingAccountsWithProofOfOwnershipRequestItem)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with oneTimePersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"loginWithoutChallenge"                   
                  }
                  "oneTimePersonaData":{
                    "fields":["givenName", "emailAddress"]
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assertEquals(AuthLoginWithoutChallengeRequestItem, item.auth)
        assert(item.oneTimePersonaData?.fields?.size == 2)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with ongoingPersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"loginWithChallenge",
                    "challenge":"randomChallenge"
                  }
                  "ongoingPersonaData":{
                    "fields":["givenName", "emailAddress"]
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assertEquals(AuthLoginWithChallengeRequestItem("randomChallenge"), item.auth)
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
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
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
        val result = peerdroidRequestJson.encodeToString(response)
        Assert.assertEquals(expected, result)
    }

}
