package com.spliteasy.app.data.dto

import kotlinx.serialization.Serializable

// Auth DTOs
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class RegisterResponse(
    val id: Int,
    val username: String,
    val email: String
)

@Serializable
data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val userId: Int,
    val username: String
)

// User DTOs
@Serializable
data class UserDto(
    val id: Int,
    val username: String,
    val email: String
)

// Group DTOs
@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class GroupResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val creatorId: Int
)

@Serializable
data class GroupListItem(
    val id: Int,
    val name: String,
    val description: String?,
    val memberCount: Int,
    val myBalance: Long
)

@Serializable
data class GroupDetailResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val members: List<UserDto>,
    val creatorId: Int
)

@Serializable
data class AddMemberRequest(
    val userId: Int
)

// Expense DTOs
@Serializable
data class CreateExpenseRequest(
    val description: String,
    val amountCents: Long,
    val paidByUserId: Int
)

@Serializable
data class ExpenseSplitDto(
    val user: UserDto,
    val shareAmountCents: Long,
    val settled: Boolean
)

@Serializable
data class ExpenseResponse(
    val id: Int,
    val description: String,
    val amountCents: Long,
    val paidBy: UserDto,
    val splits: List<ExpenseSplitDto>,
    val createdAt: Long
)

// Balance DTOs
@Serializable
data class BalanceItem(
    val fromUser: UserDto,
    val toUser: UserDto,
    val amountCents: Long
)

@Serializable
data class BalanceResponse(
    val groupName: String,
    val balances: List<BalanceItem>
)

@Serializable
data class SettleRequest(
    val fromUserId: Int,
    val toUserId: Int,
    val amountCents: Long
)

// Dashboard DTOs
@Serializable
data class DashboardGroupItem(
    val id: Int,
    val name: String,
    val myBalance: Long
)

@Serializable
data class DashboardResponse(
    val totalOwed: Long,
    val totalOwe: Long,
    val groups: List<DashboardGroupItem>
)

// Error DTOs
@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class MessageResponse(
    val message: String
)
