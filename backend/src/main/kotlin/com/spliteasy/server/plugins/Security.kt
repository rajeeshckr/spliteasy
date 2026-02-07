package com.spliteasy.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.*

object JwtConfig {
    private const val secret = "spliteasy-jwt-secret-key-change-in-production"
    private const val issuer = "spliteasy-server"
    private const val audience = "spliteasy-users"
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(userId: Int, username: String): String {
        return JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7 days
            .sign(algorithm)
    }
}

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.audience.contains("spliteasy-users")) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired token"))
            }
        }
    }
}

val ApplicationCall.userId: Int
    get() = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
        ?: throw IllegalStateException("No user ID in token")

val ApplicationCall.username: String
    get() = principal<JWTPrincipal>()?.payload?.getClaim("username")?.asString()
        ?: throw IllegalStateException("No username in token")
