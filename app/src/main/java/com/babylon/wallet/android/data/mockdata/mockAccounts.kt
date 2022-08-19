package com.babylon.wallet.android.data.mockdata

import com.babylon.wallet.android.data.AccountDto
import com.babylon.wallet.android.data.AccountDto.Companion.toUiModel

val mockAccountDtoList = listOf(
    AccountDto(
        id = "a1",
        name = "My main account",
        hash = "0x589e5cb09935F67c441AEe6AF46A365274a932e3",
        value = 19195.07F,
        currency = "$",
        assets = mockAssetDtoList
    ),
    AccountDto(
        id = "a2",
        name = "My fun account",
        hash = "0x589e5cb06635F67c441EAe6AF46A365278a932e1",
        value = 214945.5F,
        currency = "$",
        assets = mockAssetDtoList
    ),
    AccountDto(
        id = "a2",
        name = "Only NFTs",
        hash = "0x559e5cb66035F67c441EAe6AF46A474278a932e1",
        value = 821495.92F,
        currency = "$",
        assets = mockAssetWithoutXrdTokenDtoList
    )
)

val mockAccountUiList = mockAccountDtoList.map { accountDto ->
    accountDto.toUiModel()
}
