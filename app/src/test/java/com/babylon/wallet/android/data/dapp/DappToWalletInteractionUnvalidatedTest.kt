package com.babylon.wallet.android.data.dapp

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DappToWalletInteractionAccountsRequestItem
import com.radixdlt.sargon.DappToWalletInteractionAuthLoginWithChallengeRequestItem
import com.radixdlt.sargon.DappToWalletInteractionAuthRequestItem
import com.radixdlt.sargon.DappToWalletInteractionItems
import com.radixdlt.sargon.DappToWalletInteractionUnvalidated
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.WalletToDappInteractionResponse
import com.radixdlt.sargon.WalletToDappInteractionResponseItems
import com.radixdlt.sargon.WalletToDappInteractionSendTransactionResponseItem
import com.radixdlt.sargon.WalletToDappInteractionSuccessResponse
import com.radixdlt.sargon.WalletToDappInteractionTransactionResponseItems
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toJson
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class DappToWalletInteractionUnvalidatedTest {

    private val sampleDappAddress = AccountAddress.sampleMainnet.invoke()
    private val sampleIdentityAddress = IdentityAddress.sampleMainnet.invoke()
    private val interactionId = UUID.randomUUID().toString()
    private val challenge = Exactly32Bytes.sample.invoke()

    @Test
    fun `WalletInteraction unauthorized request decoding with oneTimeAccounts item`() {
        val interactionId = UUID.randomUUID().toString()
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
                   "send" : {
                       "version" : 1,
                        "blobs": [
                            "deadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafedeadbeefabbafadecafe"
                        ],
                        "transactionManifest" : "CALL_METHOD\n    Address(\"account_sim1cyvgx33089ukm2pl97pv4max0x40ruvfy4lt60yvya744cve475w0q\")\n    \"lock_fee\"\n    Decimal(\"500\")\n;\nCALL_METHOD\n    Address(\"account_sim1cyvgx33089ukm2pl97pv4max0x40ruvfy4lt60yvya744cve475w0q\")\n    \"withdraw\"\n    Address(\"resource_sim1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxakj8n3\")\n    Decimal(\"330\")\n;\nTAKE_FROM_WORKTOP\n    Address(\"resource_sim1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxakj8n3\")\n    Decimal(\"150\")\n    Bucket(\"bucket1\")\n;\nCALL_METHOD\n    Address(\"account_sim1c8mulhl5yrk6hh4jsyldps5sdrp08r5v9wusupvzxgqvhlp4c4nwjz\")\n    \"try_deposit_or_abort\"\n    Bucket(\"bucket1\")\n    Enum<0u8>()\n;\nTAKE_FROM_WORKTOP\n    Address(\"resource_sim1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxakj8n3\")\n    Decimal(\"130\")\n    Bucket(\"bucket2\")\n;\nCALL_METHOD\n    Address(\"account_sim1c8s2hass5g62ckwpv78y8ykdqljtetv4ve6etcz64gveykxznj36tr\")\n    \"try_deposit_or_abort\"\n    Bucket(\"bucket2\")\n    Enum<0u8>()\n;\nTAKE_FROM_WORKTOP\n    Address(\"resource_sim1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxakj8n3\")\n    Decimal(\"50\")\n    Bucket(\"bucket3\")\n;\nCALL_METHOD\n    Address(\"account_sim1c8ct6jdcwqrg3gzskyxuy0z933fe55fyjz6p56730r95ulzwl3ppva\")\n    \"try_deposit_or_abort\"\n    Bucket(\"bucket3\")\n    Enum<0u8>()\n;\n"
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
        val result = DappToWalletInteractionUnvalidated.fromJson(request)
        assert(result.items is DappToWalletInteractionItems.Transaction)
    }

    @Test
    fun `transaction approval response matches expected`() {
        val interacionId = UUID.randomUUID().toString()
        val expected =
            """{"discriminator":"success","interactionId":"$interacionId","items":{"discriminator":"transaction","send":{"transactionIntentHash":"txid_rdx1frcm6zzyfd08z0deu9x24sh64eccxeux4j2dv3dsqeuh9qsz4y6szm3ltd"}}}"""
        val response: WalletToDappInteractionResponse = WalletToDappInteractionResponse.Success(
            v1 = WalletToDappInteractionSuccessResponse(
                interactionId = interacionId,
                items = WalletToDappInteractionResponseItems.Transaction(
                    v1 = WalletToDappInteractionTransactionResponseItems(
                        send = WalletToDappInteractionSendTransactionResponseItem(
                            TransactionIntentHash.init("txid_rdx1frcm6zzyfd08z0deu9x24sh64eccxeux4j2dv3dsqeuh9qsz4y6szm3ltd"),
                        )
                    )
                )
            )
        )
        val result = response.toJson()
        assertEquals(expected, result)
    }

}
