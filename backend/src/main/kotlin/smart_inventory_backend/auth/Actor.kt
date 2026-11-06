package smart_inventory_backend.auth

import org.springframework.security.core.Authentication

/** Who is performing an action. Lets services stay independent of Spring Security. */
data class Actor(val username: String, val role: Role)

/** Reads the actor from an authority such as "ROLE_MANAGER". */
fun Authentication.toActor(): Actor {
    val authority = authorities.first().authority
    return Actor(name, Role.valueOf(authority.removePrefix("ROLE_")))
}
