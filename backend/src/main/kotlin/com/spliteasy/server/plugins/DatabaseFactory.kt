package com.spliteasy.server.plugins

import com.spliteasy.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object DatabaseFactory {
    fun init() {
        val databasePath = "data/spliteasy.db"
        val database = Database.connect(
            url = "jdbc:sqlite:$databasePath",
            driver = "org.sqlite.JDBC"
        )

        transaction(database) {
            SchemaUtils.create(Users, Groups, GroupMembers, Expenses, ExpenseSplits, Settlements)

            // Seed test users if none exist
            if (Users.selectAll().empty()) {
                seedTestUsers()
            }
        }
    }

    private fun seedTestUsers() {
        val testUsers = listOf(
            Triple("alice", "alice@test.com", "password1"),
            Triple("bob", "bob@test.com", "password2"),
            Triple("carol", "carol@test.com", "password3"),
            Triple("dave", "dave@test.com", "password4"),
            Triple("eve", "eve@test.com", "password5")
        )

        testUsers.forEach { (username, email, password) ->
            Users.insert {
                it[Users.username] = username
                it[Users.email] = email
                it[Users.passwordHash] = BCrypt.hashpw(password, BCrypt.gensalt())
                it[Users.createdAt] = System.currentTimeMillis()
            }
        }

        println("Seeded ${testUsers.size} test users")
    }
}
