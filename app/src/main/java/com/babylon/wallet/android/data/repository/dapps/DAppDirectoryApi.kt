package com.babylon.wallet.android.data.repository.dapps

import com.babylon.wallet.android.domain.model.DirectoryDefinition
import retrofit2.Call
import retrofit2.http.GET

interface DAppDirectoryApi {

    @GET("list")
    fun directory(): Call<List<DirectoryDefinition>>
}
