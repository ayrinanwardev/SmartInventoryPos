package com.example.smartinventorypos.data.repo

import com.example.smartinventorypos.data.remote.LoginReq
import com.example.smartinventorypos.data.remote.LoginRes
import com.example.smartinventorypos.data.remote.PosApi
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.model.Role
import com.example.smartinventorypos.domain.model.UserSession
import com.example.smartinventorypos.domain.repository.AuthRepository
import com.example.smartinventorypos.domain.repository.SessionStore
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: PosApi,
    private val sessions: SessionStore
) : AuthRepository {

    override suspend fun login(username: String, password: String): AppResult<Unit> =
        signIn { api.login(LoginReq(username, password)) }

    override suspend fun register(username: String, password: String): AppResult<Unit> =
        signIn { api.register(LoginReq(username, password)) }

    override fun logout() = sessions.clear()

    private suspend fun signIn(request: suspend () -> LoginRes): AppResult<Unit> = safeCall {
        val response = request()
        sessions.save(UserSession(response.token, Role.fromName(response.role)))
    }
}
