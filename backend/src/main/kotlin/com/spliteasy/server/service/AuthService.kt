package com.spliteasy.server.service

import com.spliteasy.server.dto.*
import com.spliteasy.server.models.Users
import com.spliteasy.server.plugins.JwtConfig
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object AuthService {
    fun register(request: RegisterRequest): Result<RegisterResponse> = transaction {
        // Validation
        if (request.username.isBlank() || request.email.isBlank() || request.password.length < 8) {
            return@transaction Result.failure(Exception("Username and email required, password min 8 chars"))
        }

        // Check if username or email exists
        val existingUser = Users.select {
            (Users.username eq request.username) or (Users.email eq request.email)
        }.singleOrNull()

        if (existingUser != null) {
            val field = if (existingUser[Users.username] == request.username) "Username" else "Email"
            return@transaction Result.failure(Exception("$field already exists"))
        }

        // Create user
        val userId = Users.insert {
            it[username] = request.username
            it[email] = request.email
            it[passwordHash] = BCrypt.hashpw(request.password, BCrypt.gensalt())
            it[createdAt] = System.currentTimeMillis()
        } get Users.id

        Result.success(RegisterResponse(userId, request.username, request.email))
    }

    fun login(request: LoginRequest): Result<LoginResponse> = transaction {
        val user = Users.select {
            (Users.username eq request.usernameOrEmail) or (Users.email eq request.usernameOrEmail)
        }.singleOrNull()

        if (user == null || !BCrypt.checkpw(request.password, user[Users.passwordHash])) {
            return@transaction Result.failure(Exception("Invalid credentials"))
        }

        val token = JwtConfig.generateToken(user[Users.id], user[Users.username])
        Result.success(LoginResponse(token, user[Users.id], user[Users.username]))
    }
}
