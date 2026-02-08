package com.spliteasy.server.models

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 60)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Groups : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val description = varchar("description", 500).nullable()
    val creatorId = integer("creator_id").references(Users.id)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object GroupMembers : Table("group_members") {
    val groupId = integer("group_id").references(Groups.id)
    val userId = integer("user_id").references(Users.id)
    val joinedAt = long("joined_at")

    override val primaryKey = PrimaryKey(groupId, userId)
}

object Expenses : Table() {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id)
    val description = varchar("description", 200)
    val amountCents = long("amount_cents")
    val paidByUserId = integer("paid_by_user_id").references(Users.id)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ExpenseSplits : Table("expense_splits") {
    val id = integer("id").autoIncrement()
    val expenseId = integer("expense_id").references(Expenses.id)
    val userId = integer("user_id").references(Users.id)
    val shareAmountCents = long("share_amount_cents")
    val settled = bool("settled").default(false)

    override val primaryKey = PrimaryKey(id)
}

object Settlements : Table() {
    val id = integer("id").autoIncrement()
    val groupId = integer("group_id").references(Groups.id)
    val fromUserId = integer("from_user_id").references(Users.id)
    val toUserId = integer("to_user_id").references(Users.id)
    val amountCents = long("amount_cents")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
