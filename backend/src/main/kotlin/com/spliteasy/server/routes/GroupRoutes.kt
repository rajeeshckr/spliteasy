package com.spliteasy.server.routes

import com.spliteasy.server.dto.*
import com.spliteasy.server.plugins.userId
import com.spliteasy.server.service.GroupService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.groupRoutes() {
    authenticate("auth-jwt") {
        route("/api/groups") {
            get {
                val groups = GroupService.getUserGroups(call.userId)
                call.respond(HttpStatusCode.OK, groups)
            }

            post {
                val request = call.receive<CreateGroupRequest>()
                val group = GroupService.createGroup(request, call.userId)
                call.respond(HttpStatusCode.Created, group)
            }

            route("/{id}") {
                get {
                    val groupId = call.parameters["id"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid group ID"))

                    if (!GroupService.isUserInGroup(groupId, call.userId)) {
                        return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Not a member of this group"))
                    }

                    val group = GroupService.getGroupDetail(groupId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Group not found"))

                    call.respond(HttpStatusCode.OK, group)
                }

                post("/members") {
                    val groupId = call.parameters["id"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid group ID"))

                    if (!GroupService.isUserInGroup(groupId, call.userId)) {
                        return@post call.respond(HttpStatusCode.Forbidden, ErrorResponse("Not a member of this group"))
                    }

                    val request = call.receive<AddMemberRequest>()

                    GroupService.addMember(groupId, request.userId).fold(
                        onSuccess = {
                            call.respond(HttpStatusCode.Created, MessageResponse("Member added"))
                        },
                        onFailure = { error ->
                            val statusCode = if (error.message?.contains("already a member") == true)
                                HttpStatusCode.Conflict else HttpStatusCode.BadRequest
                            call.respond(statusCode, ErrorResponse(error.message ?: "Failed to add member"))
                        }
                    )
                }

                delete("/members/{userId}") {
                    val groupId = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid group ID"))

                    val memberUserId = call.parameters["userId"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))

                    if (!GroupService.isUserInGroup(groupId, call.userId)) {
                        return@delete call.respond(HttpStatusCode.Forbidden, ErrorResponse("Not a member of this group"))
                    }

                    GroupService.removeMember(groupId, memberUserId).fold(
                        onSuccess = {
                            call.respond(HttpStatusCode.OK, MessageResponse("Member removed"))
                        },
                        onFailure = { error ->
                            call.respond(HttpStatusCode.BadRequest, ErrorResponse(error.message ?: "Failed to remove member"))
                        }
                    )
                }
            }
        }
    }
}
