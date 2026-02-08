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

            // Amount already settled on expenses this user paid for (by other users)
            val settledOnUserExpenses = (ExpenseSplits innerJoin Expenses)
                .select {
                    (Expenses.groupId eq groupId) and
                    (Expenses.paidByUserId eq userId) and
                    (ExpenseSplits.userId neq userId) and
                    (ExpenseSplits.settled eq true)
                }
                .sumOf { it[ExpenseSplits.shareAmountCents] }

            // Amount user owes (excluding settled splits)
            val owed = (ExpenseSplits innerJoin Expenses)
                .select {
                    (Expenses.groupId eq groupId) and
                    (ExpenseSplits.userId eq userId) and
                    (ExpenseSplits.settled eq false)
                }
                .sumOf { it[ExpenseSplits.shareAmountCents] }

            userBalances[userId] = (paid - settledOnUserExpenses) - owed
        }

        // Simplify debts
        val simplifiedDebts = simplifyDebts(userBalances)

        BalanceResponse(
            groupName = groupName,
            balances = simplifiedDebts
        )
    }

    fun settleDebt(groupId: Int, request: SettleRequest): Result<Unit> = transaction {
        // Mark all relevant splits as settled
        val expenses = Expenses.select { Expenses.groupId eq groupId }
            .map { it[Expenses.id] }

        expenses.forEach { expenseId ->
            ExpenseSplits.update({
                (ExpenseSplits.expenseId eq expenseId) and (ExpenseSplits.userId eq request.fromUserId)
            }) {
                it[settled] = true
            }
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
