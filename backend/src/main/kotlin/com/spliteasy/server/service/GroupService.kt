package com.spliteasy.server.service

import com.spliteasy.server.dto.*
import com.spliteasy.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object GroupService {
    fun createGroup(request: CreateGroupRequest, creatorId: Int): GroupResponse = transaction {
        val groupId = Groups.insert {
            it[name] = request.name
            it[description] = request.description
            it[Groups.creatorId] = creatorId
            it[createdAt] = System.currentTimeMillis()
        } get Groups.id

        // Add creator as member
        GroupMembers.insert {
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = creatorId
            it[GroupMembers.joinedAt] = System.currentTimeMillis()
        }

        GroupResponse(groupId, request.name, request.description, creatorId)
    }

    fun getUserGroups(userId: Int): List<GroupListItem> = transaction {
        (Groups innerJoin GroupMembers)
            .select { GroupMembers.userId eq userId }
            .map { row ->
                val groupId = row[Groups.id]
                val memberCount = GroupMembers.select { GroupMembers.groupId eq groupId }.count().toInt()
                val myBalance = calculateUserBalanceInGroup(groupId, userId)

                GroupListItem(
                    id = groupId,
                    name = row[Groups.name],
                    description = row[Groups.description],
                    memberCount = memberCount,
                    myBalance = myBalance
                )
            }
    }

    fun getGroupDetail(groupId: Int): GroupDetailResponse? = transaction {
        val group = Groups.select { Groups.id eq groupId }.singleOrNull() ?: return@transaction null

        val members = (GroupMembers innerJoin Users)
            .select { GroupMembers.groupId eq groupId }
            .map {
                UserDto(
                    id = it[Users.id],
                    username = it[Users.username],
                    email = it[Users.email]
                )
            }

        GroupDetailResponse(
            id = group[Groups.id],
            name = group[Groups.name],
            description = group[Groups.description],
            members = members,
            creatorId = group[Groups.creatorId]
        )
    }

    fun addMember(groupId: Int, userId: Int): Result<Unit> = transaction {
        // Check if already a member
        val existing = GroupMembers.select {
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }.singleOrNull()

        if (existing != null) {
            return@transaction Result.failure(Exception("User is already a member"))
        }

        GroupMembers.insert {
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = userId
            it[joinedAt] = System.currentTimeMillis()
        }

        Result.success(Unit)
    }

    fun removeMember(groupId: Int, userId: Int): Result<Unit> = transaction {
        val group = Groups.select { Groups.id eq groupId }.singleOrNull()
            ?: return@transaction Result.failure(Exception("Group not found"))

        if (group[Groups.creatorId] == userId) {
            return@transaction Result.failure(Exception("Cannot remove the group creator"))
        }

        GroupMembers.deleteWhere {
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }

        Result.success(Unit)
    }

    fun isUserInGroup(groupId: Int, userId: Int): Boolean = transaction {
        GroupMembers.select {
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }.singleOrNull() != null
    }

    private fun calculateUserBalanceInGroup(groupId: Int, userId: Int): Long {
        // Positive = owed to user, Negative = user owes
        val paid = Expenses.select {
            (Expenses.groupId eq groupId) and (Expenses.paidByUserId eq userId)
        }.sumOf { it[Expenses.amountCents] }

        // Amount user owes (their share of all expenses)
        val owed = (ExpenseSplits innerJoin Expenses)
            .select {
                (Expenses.groupId eq groupId) and
                (ExpenseSplits.userId eq userId)
            }
            .sumOf { it[ExpenseSplits.shareAmountCents] }

        // Amount received via settlements (from other users) - reduces what you're owed
        val settlementsReceived = Settlements.select {
            (Settlements.groupId eq groupId) and (Settlements.toUserId eq userId)
        }.sumOf { it[Settlements.amountCents] }

        // Amount paid via settlements (to other users) - reduces what you owe
        val settlementsPaid = Settlements.select {
            (Settlements.groupId eq groupId) and (Settlements.fromUserId eq userId)
        }.sumOf { it[Settlements.amountCents] }

        return (paid - owed) - settlementsReceived + settlementsPaid
    }
}
