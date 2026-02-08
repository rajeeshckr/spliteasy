package com.spliteasy.server.service

import com.spliteasy.server.dto.*
import com.spliteasy.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object BalanceService {
    fun getGroupBalances(groupId: Int): BalanceResponse = transaction {
        val group = Groups.select { Groups.id eq groupId }.single()
        val groupName = group[Groups.name]

        // Calculate net balance for each user
        val userBalances = mutableMapOf<Int, Long>()

        // Get all group members
        val members = GroupMembers.select { GroupMembers.groupId eq groupId }
            .map { it[GroupMembers.userId] }

        members.forEach { userId ->
            // Amount paid by user
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

            userBalances[userId] = (paid - owed) - settlementsReceived + settlementsPaid
        }

        // Simplify debts
        val simplifiedDebts = simplifyDebts(userBalances)

        BalanceResponse(
            groupName = groupName,
            balances = simplifiedDebts
        )
    }

    fun settleDebt(groupId: Int, request: SettleRequest): Result<Unit> = transaction {
        // Record the settlement transaction
        Settlements.insert {
            it[Settlements.groupId] = groupId
            it[fromUserId] = request.fromUserId
            it[toUserId] = request.toUserId
            it[amountCents] = request.amountCents
            it[createdAt] = System.currentTimeMillis()
        }

        Result.success(Unit)
    }

    fun getDashboard(userId: Int): DashboardResponse = transaction {
        val groups = GroupService.getUserGroups(userId)

        var totalOwed = 0L
        var totalOwe = 0L

        groups.forEach { group ->
            if (group.myBalance > 0) {
                totalOwed += group.myBalance
            } else {
                totalOwe += -group.myBalance
            }
        }

        DashboardResponse(
            totalOwed = totalOwed,
            totalOwe = totalOwe,
            groups = groups.map {
                DashboardGroupItem(
                    id = it.id,
                    name = it.name,
                    myBalance = it.myBalance
                )
            }
        )
    }

    private fun simplifyDebts(balances: Map<Int, Long>): List<BalanceItem> {
        val creditors = balances.filter { it.value > 0 }.toMutableMap()
        val debtors = balances.filter { it.value < 0 }.mapValues { -it.value }.toMutableMap()
        val result = mutableListOf<BalanceItem>()

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val (creditorId, creditAmount) = creditors.entries.first()
            val (debtorId, debtAmount) = debtors.entries.first()

            val settleAmount = minOf(creditAmount, debtAmount)

            val creditor = UserService.getUserById(creditorId) ?: continue
            val debtor = UserService.getUserById(debtorId) ?: continue

            result.add(
                BalanceItem(
                    fromUser = debtor,
                    toUser = creditor,
                    amountCents = settleAmount
                )
            )

            creditors[creditorId] = creditAmount - settleAmount
            debtors[debtorId] = debtAmount - settleAmount

            if (creditors[creditorId] == 0L) creditors.remove(creditorId)
            if (debtors[debtorId] == 0L) debtors.remove(debtorId)
        }

        return result
    }
}
