package com.babylon.wallet.android.data.dapp

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DappToWalletInteractionAccountsRequestItem
import com.radixdlt.sargon.DappToWalletInteractionAuthLoginWithChallengeRequestItem
import com.radixdlt.sargon.DappToWalletInteractionAuthRequestItem
import com.radixdlt.sargon.DappToWalletInteractionItems
import com.radixdlt.sargon.DappToWalletInteractionUnvalidated
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.WalletToDappInteractionResponseItems
import com.radixdlt.sargon.WalletToDappInteractionSendTransactionResponseItem
import com.radixdlt.sargon.WalletToDappInteractionSuccessResponse
import com.radixdlt.sargon.WalletToDappInteractionTransactionResponseItems
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toJson
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import org.junit.Assert.assertEquals
import org.junit.Test

class DappToWalletInteractionUnvalidatedTest {

    private val sampleDappAddress = AccountAddress.sampleMainnet.invoke()
    private val sampleIdentityAddress = IdentityAddress.sampleMainnet.invoke()
    private val interactionId = WalletInteractionId.randomUUID()
    private val challenge = Exactly32Bytes.sample.invoke()

    @Test
    fun `WalletInteraction unauthorized request decoding with oneTimeAccounts item`() {
        val interactionId = WalletInteractionId.randomUUID()
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
               "interactionId":"$interactionId",
               "metadata":{
                   "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.UnauthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.UnauthorizedRequest
        assert(item.v1.oneTimeAccounts is DappToWalletInteractionAccountsRequestItem)
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
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.UnauthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.UnauthorizedRequest
        assert(item.v1.oneTimePersonaData?.isRequestingName == true)
        assert(item.v1.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantity?.toInt() == 1)
        assert(item.v1.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantifier == RequestedNumberQuantifier.EXACTLY)
    }

    @Test
    fun `WalletInteraction authorized usePersona request decoding with oneTimeAccounts item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"usePersona",
                    "identityAddress":"${sampleIdentityAddress.string}"
                  },
                  "oneTimeAccounts":{
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.AuthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.AuthorizedRequest
        assert(item.v1.auth is DappToWalletInteractionAuthRequestItem.UsePersona)
        assert(item.v1.oneTimeAccounts is DappToWalletInteractionAccountsRequestItem)
    }

    @Test
    fun `WalletInteraction authorized usePersona request decoding with ongoingAccounts item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"usePersona",
                    "identityAddress":"${sampleIdentityAddress.string}"
                  },
                  "ongoingAccounts":{
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
              "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.AuthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.AuthorizedRequest
        assert(item.v1.auth is DappToWalletInteractionAuthRequestItem.UsePersona)
        assert(item.v1.ongoingAccounts is DappToWalletInteractionAccountsRequestItem)
    }

    @Test
    fun `WalletInteraction authorized usePersona request decoding with oneTimePersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"usePersona",
                    "identityAddress":"${sampleIdentityAddress.string}"
                  },
                  "oneTimePersonaData":{
                      "isRequestingName": true,
                      "numberOfRequestedEmailAddresses": {
                        "quantifier": "exactly",
                        "quantity": 1
                      }
                  }               
               },
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.AuthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.AuthorizedRequest
        assert(item.v1.oneTimePersonaData?.isRequestingName == true)
        assert(item.v1.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantity?.toInt() == 1)
        assert(item.v1.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantifier == RequestedNumberQuantifier.EXACTLY)
    }

    @Test
    fun `WalletInteraction authorized usePersona request decoding with ongoingPersonaData item`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"usePersona",
                    "identityAddress":"${sampleIdentityAddress.string}"
                  },
                  "ongoingPersonaData":{
                      "isRequestingName": true,
                      "numberOfRequestedEmailAddresses": {
                        "quantifier": "exactly",
                        "quantity": 1
                      }
                  }
               },
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.AuthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.AuthorizedRequest
        assert(item.v1.ongoingPersonaData?.isRequestingName == true)
        assert(item.v1.ongoingPersonaData?.numberOfRequestedEmailAddresses?.quantity?.toInt() == 1)
        assert(item.v1.ongoingPersonaData?.numberOfRequestedEmailAddresses?.quantifier == RequestedNumberQuantifier.EXACTLY)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with oneTimeAccounts item`() {
        val challenge = Exactly32Bytes.sample.invoke()
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"loginWithoutChallenge"                   
                  },
                  "oneTimeAccounts":{
                     "challenge": "${challenge.hex}",
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.AuthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.AuthorizedRequest
        assert(item.v1.auth is DappToWalletInteractionAuthRequestItem.LoginWithoutChallenge)
        assert(item.v1.oneTimeAccounts is DappToWalletInteractionAccountsRequestItem)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with ongoingAccounts item without proof of ownership`() {
        val challenge = Exactly32Bytes.sample.invoke()
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"loginWithChallenge",
                    "challenge":"${challenge.hex}"
                  },
                  "ongoingAccounts":{
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.AuthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.AuthorizedRequest
        assertEquals(
            DappToWalletInteractionAuthRequestItem.LoginWithChallenge(
                v1 = DappToWalletInteractionAuthLoginWithChallengeRequestItem(challenge)
            ), item.v1.auth
        )
        assert(item.v1.ongoingAccounts is DappToWalletInteractionAccountsRequestItem)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with ongoingAccounts item with proof of ownership`() {
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                  "auth":{
                    "discriminator":"loginWithChallenge",
                    "challenge":"${challenge.hex}"
                  },
                  "ongoingAccounts":{
                     "challenge":"${challenge.hex}",
                     "numberOfAccounts":{
                        "quantity":1,
                        "quantifier":"exactly"
                     }
                  }               
               },
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works/",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.AuthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.AuthorizedRequest
        assertEquals(
            DappToWalletInteractionAuthRequestItem.LoginWithChallenge(
                v1 = DappToWalletInteractionAuthLoginWithChallengeRequestItem(challenge)
            ), item.v1.auth
        )
        assert(item.v1.ongoingAccounts is DappToWalletInteractionAccountsRequestItem)
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
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.AuthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.AuthorizedRequest
        assert(item.v1.auth is DappToWalletInteractionAuthRequestItem.LoginWithoutChallenge)
        assert(item.v1.oneTimePersonaData?.isRequestingName == true)
        assert(item.v1.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantity?.toInt() == 1)
        assert(item.v1.oneTimePersonaData?.numberOfRequestedEmailAddresses?.quantifier == RequestedNumberQuantifier.EXACTLY)
    }

    @Test
    fun `WalletInteraction authorized login request decoding with ongoingPersonaData item`() {
        val challenge = Exactly32Bytes.sample.invoke()
        val request = """
            {
               "items":{
                  "discriminator":"authorizedRequest",
                   "auth":{
                    "discriminator":"loginWithChallenge",
                    "challenge":"${challenge.hex}"
                  },
                  "ongoingPersonaData":{
                      "isRequestingName": true,
                      "numberOfRequestedEmailAddresses": {
                        "quantifier": "exactly",
                        "quantity": 1
                      }
                  }
               },
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.AuthorizedRequest)
        val item = result.items as DappToWalletInteractionItems.AuthorizedRequest
        assertEquals(
            DappToWalletInteractionAuthRequestItem.LoginWithChallenge(
                v1 = DappToWalletInteractionAuthLoginWithChallengeRequestItem(challenge)
            ), item.v1.auth
        )
        assert(item.v1.ongoingPersonaData?.isRequestingName == true)
        assert(item.v1.ongoingPersonaData?.numberOfRequestedEmailAddresses?.quantity?.toInt() == 1)
        assert(item.v1.ongoingPersonaData?.numberOfRequestedEmailAddresses?.quantifier == RequestedNumberQuantifier.EXACTLY)
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
               "interactionId":"$interactionId",
               "metadata":{
                  "version": 1,
                  "networkId":1,
                  "origin":"https://dashboard-hammunet.rdx-works-main.extratools.works",
                  "dAppDefinitionAddress":"${sampleDappAddress.string}"
               }
            }
        """
        val result = DappToWalletInteractionUnvalidated.Companion.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.Transaction)
        val item = result.items as DappToWalletInteractionItems.Transaction
        assert(item.v1.send.transactionManifest == "manifest")
    }

    @Test
    fun `transaction approval response matches expected`() {
        val interacionId = WalletInteractionId.randomUUID()
        val expected =
            """{"discriminator":"success","interactionId":"$interacionId","items":{"discriminator":"transaction","send":{"transactionIntentHash":"1"}}}"""
        val response: WalletToDappInteractionResponse = WalletToDappInteractionResponse.Success(
            v1 = WalletToDappInteractionSuccessResponse(
                interactionId = interacionId,
                items = WalletToDappInteractionResponseItems.Transaction(
                    v1 = WalletToDappInteractionTransactionResponseItems(
                        send = WalletToDappInteractionSendTransactionResponseItem(
                            bech32EncodedTxId = "1",
                        )
                    )
                )
            )
        )
        val result = response.toJson()
        assertEquals(expected, result)
    }

}
