package com.spliteasy.server.service

import com.spliteasy.server.dto.UserDto
import com.spliteasy.server.models.Users
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object UserService {
    fun searchUsers(query: String): List<UserDto> = transaction {
        if (query.isBlank()) return@transaction emptyList()

        Users.select {
            (Users.username.lowerCase() like "%${query.lowercase()}%") or
            (Users.email.lowerCase() like "%${query.lowercase()}%")
        }.map {
            UserDto(
                id = it[Users.id],
                username = it[Users.username],
                email = it[Users.email]
            )
        }
    }

    fun getUserById(userId: Int): UserDto? = transaction {
        Users.select { Users.id eq userId }.singleOrNull()?.let {
            UserDto(
                id = it[Users.id],
                username = it[Users.username],
                email = it[Users.email]
            )
        }
    }
}
