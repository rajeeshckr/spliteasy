package com.spliteasy.server.routes

import com.spliteasy.server.dto.*
import com.spliteasy.server.service.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    route("/api/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()

            AuthService.register(request).fold(
                onSuccess = { response ->
                    call.respond(HttpStatusCode.Created, response)
                },
                onFailure = { error ->
                    val statusCode = if (error.message?.contains("already exists") == true)
                        HttpStatusCode.Conflict else HttpStatusCode.BadRequest
                    call.respond(statusCode, ErrorResponse(error.message ?: "Registration failed"))
                }
            )
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            AuthService.login(request).fold(
                onSuccess = { response ->
                    call.respond(HttpStatusCode.OK, response)
                },
                onFailure = { error ->
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(error.message ?: "Login failed"))
                }
            )
        }
    }
}
