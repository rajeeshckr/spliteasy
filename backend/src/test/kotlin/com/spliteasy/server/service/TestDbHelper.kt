package com.spliteasy.server.service

import com.spliteasy.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Helper for setting up an H2 in-memory database for tests.
 * H2 works reliably with Exposed's connection pooling.
 */
object TestDbHelper {
    private var initialized = false

    fun initTestDb() {
        if (!initialized) {
            // H2 in-memory with DB_CLOSE_DELAY=-1 keeps DB alive as long as JVM runs
            Database.connect(
                "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL",
                driver = "org.h2.Driver"
            )
            transaction {
                SchemaUtils.create(Users, Groups, GroupMembers, Expenses, ExpenseSplits)
            }
            initialized = true
        }
    }

    fun cleanAll() {
        transaction {
            ExpenseSplits.deleteAll()
            Expenses.deleteAll()
            GroupMembers.deleteAll()
            Groups.deleteAll()
            Users.deleteAll()
        }
    }

    fun createUser(username: String, email: String): Int = transaction {
        Users.insert {
            it[Users.username] = username
            it[Users.email] = email
            it[Users.passwordHash] = "hash"
            it[Users.createdAt] = System.currentTimeMillis()
        } get Users.id
    }

    fun createGroup(name: String, creatorId: Int): Int = transaction {
        val groupId = Groups.insert {
            it[Groups.name] = name
            it[Groups.description] = null
            it[Groups.creatorId] = creatorId
            it[Groups.createdAt] = System.currentTimeMillis()
        } get Groups.id

        GroupMembers.insert {
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = creatorId
            it[GroupMembers.joinedAt] = System.currentTimeMillis()
        }

        groupId
    }

    fun addMemberToGroup(groupId: Int, userId: Int) = transaction {
        GroupMembers.insert {
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = userId
            it[GroupMembers.joinedAt] = System.currentTimeMillis()
        }
    }

    fun createExpense(groupId: Int, description: String, amountCents: Long, paidByUserId: Int): Int = transaction {
        Expenses.insert {
            it[Expenses.groupId] = groupId
            it[Expenses.description] = description
            it[Expenses.amountCents] = amountCents
            it[Expenses.paidByUserId] = paidByUserId
            it[Expenses.createdAt] = System.currentTimeMillis()
        } get Expenses.id
    }

    fun createSplit(expenseId: Int, userId: Int, shareAmountCents: Long, settled: Boolean = false) = transaction {
        ExpenseSplits.insert {
            it[ExpenseSplits.expenseId] = expenseId
            it[ExpenseSplits.userId] = userId
            it[ExpenseSplits.shareAmountCents] = shareAmountCents
            it[ExpenseSplits.settled] = settled
        }
    }
}
