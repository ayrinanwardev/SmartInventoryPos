package com.example.smartinventorypos.data.remote

import com.example.smartinventorypos.domain.repository.SessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Adds the login token to requests, and ends the session when the server rejects that token. */
class AuthInterceptor @Inject constructor(private val sessions: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessions.session.value?.token
        val request = chain.request().newBuilder().apply {
            token?.let { header("Authorization", "Bearer $it") }
        }.build()
        val response = chain.proceed(request)
        if (response.code() == 401 && token != null) sessions.clearIfCurrent(token)
        return response
    }
}
