package com.example.smartinventorypos.data.remote
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PosApi {
    // auth
    @POST("api/auth/login") suspend fun login(@Body r: LoginReq): LoginRes
    @POST("api/auth/register") suspend fun register(@Body r: LoginReq): LoginRes

    // products
    @GET("api/products") suspend fun products(): List<ProductDto>
    @GET("api/products/low-stock") suspend fun lowStock(): List<ProductDto>
    @POST("api/products") suspend fun createProduct(@Body r: ProductReq): ProductDto
    @PUT("api/products/{id}") suspend fun updateProduct(@Path("id") id: Long, @Body r: ProductReq): ProductDto
    @DELETE("api/products/{id}") suspend fun deleteProduct(@Path("id") id: Long)
    @POST("api/products/{id}/stock") suspend fun adjustStock(@Path("id") id: Long, @Body r: StockAdjustReq): ProductDto

    // sales
    @POST("api/sales") suspend fun createSale(@Body r: SaleReq): SaleRes
    @GET("api/sales") suspend fun salesHistory(): List<SaleHistoryDto>
    @GET("api/sales/mine") suspend fun myOrders(): List<SaleHistoryDto>
    @GET("api/sales/report/today") suspend fun todayTotal(): Double
}