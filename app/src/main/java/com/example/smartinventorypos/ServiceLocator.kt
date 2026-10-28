package com.example.smartinventorypos

import android.app.Application
import android.content.Context
import androidx.room3.Room
import com.example.smartinventorypos.data.local.database.AppDatabase
import com.example.smartinventorypos.data.remote.PosApi
import com.example.smartinventorypos.data.remote.Session
import com.example.smartinventorypos.data.repo.PosRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceLocator {
    private const val BASE_URL = "http://10.0.2.2:8080/"   // emulator -> your PC's localhost
    lateinit var repo: PosRepository; private set

    fun init(ctx: Context) {
        Session.init(ctx)
        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, "pos.db").build()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val b = chain.request().newBuilder()
            Session.token?.let { b.header("Authorization", "Bearer $it") }
            chain.proceed(b.build())
        }.build()
        val api = Retrofit.Builder().baseUrl(BASE_URL).client(http)
            .addConverterFactory(GsonConverterFactory.create()).build().create(PosApi::class.java)
        repo = PosRepository(ctx.applicationContext, db, api)
    }
}

class PosApp : Application() {
    override fun onCreate() { super.onCreate(); ServiceLocator.init(this) }
}