package com.spliteasy.server.routes

import com.spliteasy.server.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    authenticate("auth-jwt") {
        route("/api/users") {
            get("/search") {
                val query = call.request.queryParameters["q"] ?: ""
                val users = UserService.searchUsers(query)
                call.respond(HttpStatusCode.OK, users)
            }
        }
    }
}
