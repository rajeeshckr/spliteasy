package com.spliteasy.server.routes

import com.spliteasy.server.dto.*
import com.spliteasy.server.plugins.userId
import com.spliteasy.server.service.BalanceService
import com.spliteasy.server.service.GroupService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.balanceRoutes() {
    authenticate("auth-jwt") {
        route("/api/groups/{id}") {
            get("/balances") {
                val groupId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid group ID"))

                if (!GroupService.isUserInGroup(groupId, call.userId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Not a member of this group"))
                }

                val balances = BalanceService.getGroupBalances(groupId)
                call.respond(HttpStatusCode.OK, balances)
            }

            post("/settle") {
                val groupId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid group ID"))

                if (!GroupService.isUserInGroup(groupId, call.userId)) {
                    return@post call.respond(HttpStatusCode.Forbidden, ErrorResponse("Not a member of this group"))
                }

                val request = call.receive<SettleRequest>()

                BalanceService.settleDebt(groupId, request).fold(
                    onSuccess = {
                        call.respond(HttpStatusCode.OK, MessageResponse("Settlement recorded"))
                    },
                    onFailure = { error ->
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(error.message ?: "Failed to settle debt"))
                    }
                )
            }
        }

        get("/api/dashboard") {
            val dashboard = BalanceService.getDashboard(call.userId)
            call.respond(HttpStatusCode.OK, dashboard)
        }
    }
}
