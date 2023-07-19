package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.AccountProof
import com.babylon.wallet.android.data.dapp.model.AccountsRequestItem
import com.babylon.wallet.android.data.dapp.model.AccountsRequestResponseItem
import com.babylon.wallet.android.data.dapp.model.AuthLoginWithChallengeRequestItem
import com.babylon.wallet.android.data.dapp.model.AuthLoginWithoutChallengeRequestItem
import com.babylon.wallet.android.data.dapp.model.AuthUsePersonaRequestItem
import com.babylon.wallet.android.data.dapp.model.NumberOfValues
import com.babylon.wallet.android.data.dapp.model.Proof
import com.babylon.wallet.android.data.dapp.model.WalletAuthorizedRequestItems
import com.babylon.wallet.android.data.dapp.model.WalletInteraction
import com.babylon.wallet.android.data.dapp.model.WalletInteractionResponse
import com.babylon.wallet.android.data.dapp.model.WalletInteractionSuccessResponse
import com.babylon.wallet.android.data.dapp.model.WalletTransactionItems
import com.babylon.wallet.android.data.dapp.model.WalletTransactionResponseItems
import com.babylon.wallet.android.data.dapp.model.WalletUnauthorizedRequestItems
import com.babylon.wallet.android.data.dapp.model.peerdroidRequestJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletInteractionModelsTest {

    @Test
    fun `OneTimeAccountsRequestResponseItem serialization & deserialization`() {
        val responseItem1 = AccountsRequestResponseItem(
            accounts = listOf(
                AccountsRequestResponseItem.Account(
                    address = "address1",
                    label = "Test account 1",
                    appearanceId = 1
                ),
                AccountsRequestResponseItem.Account(
                    address = "address2",
                    label = "Test account 2",
                    appearanceId = 2
                )
            ),
            challenge = "challenge",
            proofs = listOf(
                AccountProof(
                    accountAddress = "address1",
                    proof = Proof(
                        publicKey = "publicKey1",
                        signature = "signature1",
                        curve = Proof.Curve.Curve25519
                    )
                ),
                AccountProof(
                    accountAddress = "address2",
                    proof = Proof(
                        publicKey = "publicKey2",
                        signature = "signature2",
                        curve = Proof.Curve.Secp256k1
                    )
                )
            )
        )
        val responseItem2 = AccountsRequestResponseItem(
            accounts = listOf(
                AccountsRequestResponseItem.Account(
                    address = "address1",
                    label = "Test account",
                    appearanceId = 0
                ),
                AccountsRequestResponseItem.Account(
                    address = "address2",
                    label = "Test account",
                    appearanceId = 1
                )
            ),
            challenge = null,
            proofs = null
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
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                   "version": 1,
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletUnauthorizedRequestItems)
        val item = result.items as WalletUnauthorizedRequestItems
        assert(item.oneTimeAccounts is AccountsRequestItem)
    }

    @Test
    fun `WalletInteraction unauthorized request decoding with oneTimePersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"unauthorizedRequest",
                  "oneTimePersonaData":{
                      "isRequestingName": true,
                      "numberOfRequestedEmailAddresses": {
                        "quantifier": "exactly",
                        "quantity": 1
                      }
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletUnauthorizedRequestItems)
        val item = result.items as WalletUnauthorizedRequestItems
        assert(item.oneTimePersonaData?.isRequestingName == true)
        assert(item.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantity == 1)
        assert(item.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantifier == NumberOfValues.Quantifier.Exactly)
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
                  },
                  "oneTimeAccounts":{
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
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
        assert(item.oneTimeAccounts is AccountsRequestItem)
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
                  },
                  "ongoingAccounts":{
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
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
        assert(item.ongoingAccounts is AccountsRequestItem)
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
                  },
                  "oneTimePersonaData":{
                      "isRequestingName": true,
                      "numberOfRequestedEmailAddresses": {
                        "quantifier": "exactly",
                        "quantity": 1
                      }
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.oneTimePersonaData?.isRequestingName == true)
        assert(item.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantity == 1)
        assert(item.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantifier == NumberOfValues.Quantifier.Exactly)
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
                  },
                  "ongoingPersonaData":{
                      "isRequestingName": true,
                      "numberOfRequestedEmailAddresses": {
                        "quantifier": "exactly",
                        "quantity": 1
                      }
                  }
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
                  "networkId":34,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"dashboard"
               }
            }
        """
        val result = peerdroidRequestJson.decodeFromString<WalletInteraction>(request)
        assert(result.items is WalletAuthorizedRequestItems)
        val item = result.items as WalletAuthorizedRequestItems
        assert(item.ongoingPersonaData?.isRequestingName == true)
        assert(item.ongoingPersonaData?.numberOfRequestedEmailAddresses?.quantity == 1)
        assert(item.ongoingPersonaData?.numberOfRequestedEmailAddresses?.quantifier == NumberOfValues.Quantifier.Exactly)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with oneTimeAccounts item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"loginWithoutChallenge"                   
                  },
                  "oneTimeAccounts":{
                     "challenge": "challenge",
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
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
        assert(item.oneTimeAccounts is AccountsRequestItem)
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
                  },
                  "ongoingAccounts":{
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
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
        assert(item.ongoingAccounts is AccountsRequestItem)
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
                  },
                  "ongoingAccounts":{
                     "challenge":"challenge",
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
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
        assert(item.ongoingAccounts is AccountsRequestItem)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with oneTimePersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"loginWithoutChallenge"                   
                  },
                  "oneTimePersonaData":{
                      "isRequestingName": true,
                      "numberOfRequestedEmailAddresses": {
                        "quantifier": "exactly",
                        "quantity": 1
                      }
                  }
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
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
        assert(item.oneTimePersonaData?.isRequestingName == true)
        assert(item.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantity == 1)
        assert(item.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantifier == NumberOfValues.Quantifier.Exactly)
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
                  },
                  "ongoingPersonaData":{
                      "isRequestingName": true,
                      "numberOfRequestedEmailAddresses": {
                        "quantifier": "exactly",
                        "quantity": 1
                      }
                  }
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
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
        assert(item.ongoingPersonaData?.isRequestingName == true)
        assert(item.ongoingPersonaData?.numberOfRequestedEmailAddresses?.quantity == 1)
        assert(item.ongoingPersonaData?.numberOfRequestedEmailAddresses?.quantifier == NumberOfValues.Quantifier.Exactly)
    }

    @Test
    fun `WalletInteraction request decoding with transaction item`() {
        val request = """
            {
               "items":{
                  "discriminator":"transaction",
                  "send":{
                      "transactionManifest":"manifest",
                      "version":1,
                      "blobs":["blob1", "blob2"],
                      "message":"manifest"
                  }                                  
               },
               "interactionId":"4abe2cb1-93e2-467d-a854-5e2cec897c50",
               "metadata":{
                  "version": 1,
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
            interactionId = "1", items = WalletTransactionResponseItems(
                WalletTransactionResponseItems.SendTransactionResponseItem(
                    "1"
                )
            )
        )
        val result = peerdroidRequestJson.encodeToString(response)
        assertEquals(expected, result)
    }

}
