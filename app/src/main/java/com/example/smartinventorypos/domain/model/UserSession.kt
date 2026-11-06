package com.example.smartinventorypos.domain.model

class UserSession(val token: String, val role: Role?) {
    /** Keeps the token out of logs. */
    override fun toString() = "UserSession(role=$role)"
}
