package com.spliteasy.server.service

import com.spliteasy.server.dto.*
import com.spliteasy.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object ExpenseService {
    fun createExpense(groupId: Int, request: CreateExpenseRequest): ExpenseResponse = transaction {
        // Get all group members
        val members = GroupMembers.select { GroupMembers.groupId eq groupId }
            .map { it[GroupMembers.userId] }

        if (members.isEmpty()) {
            throw Exception("No members in group")
        }

        // Create expense
        val expenseId = Expenses.insert {
            it[Expenses.groupId] = groupId
            it[description] = request.description
            it[amountCents] = request.amountCents
            it[paidByUserId] = request.paidByUserId
            it[createdAt] = System.currentTimeMillis()
        } get Expenses.id

        // Split equally among all members
        val sharePerPerson = request.amountCents / members.size
        val remainder = request.amountCents % members.size

        members.forEachIndexed { index, memberId ->
            val share = sharePerPerson + (if (index == 0) remainder else 0)
            ExpenseSplits.insert {
                it[ExpenseSplits.expenseId] = expenseId
                it[ExpenseSplits.userId] = memberId
                it[ExpenseSplits.shareAmountCents] = share
                it[ExpenseSplits.settled] = false
            }
        }

        getExpenseById(expenseId) ?: throw Exception("Failed to create expense")
    }

    fun getGroupExpenses(groupId: Int): List<ExpenseResponse> = transaction {
        Expenses.select { Expenses.groupId eq groupId }
            .orderBy(Expenses.createdAt to SortOrder.DESC)
            .mapNotNull { getExpenseById(it[Expenses.id]) }
    }

    fun deleteExpense(groupId: Int, expenseId: Int): Result<Unit> = transaction {
        val expense = Expenses.select { Expenses.id eq expenseId }.singleOrNull()
            ?: return@transaction Result.failure(Exception("Expense not found"))

        if (expense[Expenses.groupId] != groupId) {
            return@transaction Result.failure(Exception("Expense not in this group"))
        }

        ExpenseSplits.deleteWhere { ExpenseSplits.expenseId eq expenseId }
        Expenses.deleteWhere { Expenses.id eq expenseId }

        Result.success(Unit)
    }

    private fun getExpenseById(expenseId: Int): ExpenseResponse? = transaction {
        val expense = Expenses.select { Expenses.id eq expenseId }.singleOrNull() ?: return@transaction null

        val paidBy = UserService.getUserById(expense[Expenses.paidByUserId]) ?: return@transaction null

        val splits = ExpenseSplits.select { ExpenseSplits.expenseId eq expenseId }
            .mapNotNull { split ->
                val user = UserService.getUserById(split[ExpenseSplits.userId]) ?: return@mapNotNull null
                ExpenseSplitDto(
                    user = user,
                    shareAmountCents = split[ExpenseSplits.shareAmountCents],
                    settled = split[ExpenseSplits.settled]
                )
            }

        ExpenseResponse(
            id = expense[Expenses.id],
            description = expense[Expenses.description],
            amountCents = expense[Expenses.amountCents],
            paidBy = paidBy,
            splits = splits,
            createdAt = expense[Expenses.createdAt]
        )
    }
}
