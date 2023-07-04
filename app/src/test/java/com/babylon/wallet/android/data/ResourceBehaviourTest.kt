package com.babylon.wallet.android.data

import com.babylon.wallet.android.data.gateway.extensions.calculateResourceBehaviours
import com.babylon.wallet.android.data.gateway.generated.models.AccessRulesChain
import com.babylon.wallet.android.data.gateway.generated.models.AccessChainRules
import com.babylon.wallet.android.data.gateway.generated.models.Key
import com.babylon.wallet.android.data.gateway.generated.models.Rule
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseFungibleResourceDetails
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.domain.model.behaviours.ResourceBehaviour
import org.junit.Assert
import org.junit.Test

class ResourceBehaviourTest {

    @Test
    fun `given update_non_fungible_data perform and change rules set to defaults, verify no behaviours`() {
        // given
        val expectedBehaviours = emptyList<ResourceBehaviour>()
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_non_fungible_data change rules set to NON defaults, verify CHANGE update_non_fungible_data behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_UPDATE_NON_FUNGIBLE_DATA
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_non_fungible_data perform rules set to NON defaults, verify PERFORM update_non_fungible_data behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_UPDATE_NON_FUNGIBLE_DATA
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_non_fungible_data perform and change rules set to NON defaults, verify PERFORM and CHANGE update_non_fungible_data behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_UPDATE_NON_FUNGIBLE_DATA,
            ResourceBehaviour.CHANGE_UPDATE_NON_FUNGIBLE_DATA
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_non_fungible_data perform and change rules set to NON defaults and resource is fungible, verify no update_non_fungible_data behaviors`() {
        // given
        val expectedBehaviours = emptyList<ResourceBehaviour>()
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given recall perform rules set to NON defaults, verify PERFORM recall behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_RECALL
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given recall change rules set to NON defaults, verify CHANGE recall behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_RECALL
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given recall perform and change rules set to NON defaults, verify PERFORM and CHANGE recall behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_RECALL,
            ResourceBehaviour.CHANGE_RECALL
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_metadata perform rules set to NON defaults, verify PERFORM update_metadata behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_UPDATE_METADATA
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_metadata change rules set to NON defaults, verify CHANGE update_metadata behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_UPDATE_METADATA
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given update_metadata perform and change rules set to NON defaults, verify PERFORM and CHANGE update_metadata behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_UPDATE_METADATA,
            ResourceBehaviour.CHANGE_UPDATE_METADATA
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given mint perform rules set to NON defaults, verify PERFORM mint behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_MINT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given mint change rules set to NON defaults, verify CHANGE mint behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_MINT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given mint perform and change rules set to NON defaults, verify PERFORM and CHANGE mint behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_MINT,
            ResourceBehaviour.CHANGE_MINT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn perform rules set to NON defaults, verify PERFORM burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn change rules set to NON defaults, verify CHANGE burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn perform and change rules set to NON defaults, verify PERFORM and CHANGE burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_BURN,
            ResourceBehaviour.CHANGE_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn and mint perform rules set to NON defaults, verify PERFORM mint and burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.PERFORM_MINT_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given burn and mint change rules set to NON defaults, verify CHANGE mint and burn behaviors`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_MINT_BURN
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given perform withdraw or deposit rules set to NON defaults, verify CANNOT_PERFORM_WITHDRAW_DEPOSIT behavior`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CANNOT_PERFORM_WITHDRAW_DEPOSIT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given perform withdraw and deposit rules set to defaults and either change withdraw or deposit is NON default exactly is AllowAll, verify CHANGE_WITHDRAW_DEPOSIT behaviour`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.CHANGE_WITHDRAW_DEPOSIT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given perform withdraw and deposit rules set to defaults and either change withdraw or deposit is NON default but neither is AllowAll, verify FUTURE_MOVEMENT_WITHDRAW_DEPOSIT behaviour`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.FUTURE_MOVEMENT_WITHDRAW_DEPOSIT
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "Protected"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "Protected"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given resource is fungible and all rules are set to defaults, verify DEFAULT_RESOURCE behaviour`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.DEFAULT_RESOURCE
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.fungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }

    @Test
    fun `given resource is nft and all rules are set to defaults, verify DEFAULT_RESOURCE behaviour`() {
        // given
        val expectedBehaviours = listOf(
            ResourceBehaviour.DEFAULT_RESOURCE
        )
        val fungibleResource = StateEntityDetailsResponseFungibleResourceDetails(
            type = StateEntityDetailsResponseItemDetailsType.nonFungibleResource,
            accessRulesChain = AccessRulesChain(
                rules = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "AllowAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
                mutability = listOf(
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "mint"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "burn"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "deposit"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "withdraw"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "set"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "recall"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    ),
                    AccessChainRules(
                        key = Key(
                            module = "Main",
                            name = "update_non_fungible_data"
                        ),
                        rule = Rule(
                            type = "DenyAll"
                        )
                    )
                ),
            ),
            divisibility = 1,
            totalSupply = "",
            totalBurned = "",
            totalMinted = ""
        )

        // when
        val behaviours = fungibleResource.calculateResourceBehaviours()

        // then
        Assert.assertEquals(expectedBehaviours, behaviours)
    }
}
