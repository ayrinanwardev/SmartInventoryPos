package com.example.smartinventorypos.di

import android.os.Build
import com.example.smartinventorypos.BuildConfig
import com.example.smartinventorypos.data.local.dao.ProductDao
import com.example.smartinventorypos.data.local.dao.SaleDao
import com.example.smartinventorypos.data.local.database.PosDatabase
import com.example.smartinventorypos.data.remote.AuthInterceptor
import com.example.smartinventorypos.data.remote.PosApi
import com.example.smartinventorypos.data.repo.AuthRepositoryImpl
import com.example.smartinventorypos.data.repo.OfflineSaleRepositoryImpl
import com.example.smartinventorypos.data.repo.ProductRepositoryImpl
import com.example.smartinventorypos.data.repo.SaleHistoryRepositoryImpl
import com.example.smartinventorypos.data.session.EncryptedSessionStore
import com.example.smartinventorypos.domain.repository.AuthRepository
import com.example.smartinventorypos.domain.repository.OfflineSaleRepository
import com.example.smartinventorypos.domain.repository.ProductAdminRepository
import com.example.smartinventorypos.domain.repository.ProductCatalogRepository
import com.example.smartinventorypos.domain.repository.SaleHistoryRepository
import com.example.smartinventorypos.domain.repository.SaleSyncScheduler
import com.example.smartinventorypos.domain.repository.SessionStore
import com.example.smartinventorypos.sync.WorkManagerSaleSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideProductDao(db: PosDatabase): ProductDao = db.productDao()

    @Provides
    fun provideSaleDao(db: PosDatabase): SaleDao = db.saleDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /**
     * pos.api.url from local.properties if set. Otherwise:
     * emulator -> 10.0.2.2 (the PC running the backend);
     * phone -> localhost, which needs `adb reverse tcp:8080 tcp:8080` over USB.
     */
    private val baseUrl: String
        get() = BuildConfig.API_BASE_URL.ifBlank {
            if (isEmulator) "http://10.0.2.2:8080/" else "http://localhost:8080/"
        }.trimEnd('/') + "/"

    private val isEmulator: Boolean
        get() = Build.HARDWARE in setOf("ranchu", "goldfish") || Build.FINGERPRINT.startsWith("generic")

    @Provides
    @Singleton
    fun providePosApi(authInterceptor: AuthInterceptor): PosApi {
        val client = OkHttpClient.Builder().addInterceptor(authInterceptor).build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PosApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun sessionStore(impl: EncryptedSessionStore): SessionStore
    @Binds abstract fun authRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun catalogRepository(impl: ProductRepositoryImpl): ProductCatalogRepository
    @Binds abstract fun adminRepository(impl: ProductRepositoryImpl): ProductAdminRepository
    @Binds abstract fun offlineSaleRepository(impl: OfflineSaleRepositoryImpl): OfflineSaleRepository
    @Binds abstract fun saleHistoryRepository(impl: SaleHistoryRepositoryImpl): SaleHistoryRepository
    @Binds abstract fun saleSyncScheduler(impl: WorkManagerSaleSyncScheduler): SaleSyncScheduler
}
