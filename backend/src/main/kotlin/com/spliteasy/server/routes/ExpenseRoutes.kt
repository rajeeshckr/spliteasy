package com.spliteasy.server.routes

import com.spliteasy.server.dto.*
import com.spliteasy.server.plugins.userId
import com.spliteasy.server.service.ExpenseService
import com.spliteasy.server.service.GroupService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.expenseRoutes() {
    authenticate("auth-jwt") {
        route("/api/groups/{id}/expenses") {
            get {
                val groupId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid group ID"))

                if (!GroupService.isUserInGroup(groupId, call.userId)) {
                    return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Not a member of this group"))
                }

                val expenses = ExpenseService.getGroupExpenses(groupId)
                call.respond(HttpStatusCode.OK, expenses)
            }

            post {
                val groupId = call.parameters["id"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid group ID"))

                if (!GroupService.isUserInGroup(groupId, call.userId)) {
                    return@post call.respond(HttpStatusCode.Forbidden, ErrorResponse("Not a member of this group"))
                }

                val request = call.receive<CreateExpenseRequest>()

                try {
                    val expense = ExpenseService.createExpense(groupId, request)
                    call.respond(HttpStatusCode.Created, expense)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Failed to create expense"))
                }
            }

            delete("/{expenseId}") {
                val groupId = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid group ID"))

                val expenseId = call.parameters["expenseId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid expense ID"))

                if (!GroupService.isUserInGroup(groupId, call.userId)) {
                    return@delete call.respond(HttpStatusCode.Forbidden, ErrorResponse("Not a member of this group"))
                }

                ExpenseService.deleteExpense(groupId, expenseId).fold(
                    onSuccess = {
                        call.respond(HttpStatusCode.OK, MessageResponse("Expense deleted"))
                    },
                    onFailure = { error ->
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(error.message ?: "Failed to delete expense"))
                    }
                )
            }
        }
    }
}
