package com.example.smartinventorypos.data.remote
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PosApi {
    @POST("api/auth/login") suspend fun login(@Body r: LoginReq): LoginRes
    @GET("api/products") suspend fun products(): List<ProductDto>
    @POST("api/sales") suspend fun createSale(@Body r: SaleReq): SaleRes
}