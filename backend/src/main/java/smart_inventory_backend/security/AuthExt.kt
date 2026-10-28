package smart_inventory_backend.security

import org.springframework.security.core.Authentication

/** "ROLE_MANAGER" -> "MANAGER" */
fun Authentication.roleName(): String =
    authorities.firstOrNull()?.authority?.removePrefix("ROLE_") ?: ""