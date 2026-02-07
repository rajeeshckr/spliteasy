package com.spliteasy.app.data.api

import com.spliteasy.app.data.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Users
    @GET("/api/users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<UserDto>>

    // Groups
    @GET("/api/groups")
    suspend fun getGroups(): Response<List<GroupListItem>>

    @POST("/api/groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<GroupResponse>

    @GET("/api/groups/{id}")
    suspend fun getGroup(@Path("id") groupId: Int): Response<GroupDetailResponse>

    @POST("/api/groups/{id}/members")
    suspend fun addMember(
        @Path("id") groupId: Int,
        @Body request: AddMemberRequest
    ): Response<MessageResponse>

    @DELETE("/api/groups/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") groupId: Int,
        @Path("userId") userId: Int
    ): Response<MessageResponse>

    // Expenses
    @GET("/api/groups/{id}/expenses")
    suspend fun getExpenses(@Path("id") groupId: Int): Response<List<ExpenseResponse>>

    @POST("/api/groups/{id}/expenses")
    suspend fun createExpense(
        @Path("id") groupId: Int,
        @Body request: CreateExpenseRequest
    ): Response<ExpenseResponse>

    @DELETE("/api/groups/{id}/expenses/{expenseId}")
    suspend fun deleteExpense(
        @Path("id") groupId: Int,
        @Path("expenseId") expenseId: Int
    ): Response<MessageResponse>

    // Balances
    @GET("/api/groups/{id}/balances")
    suspend fun getBalances(@Path("id") groupId: Int): Response<BalanceResponse>

    @POST("/api/groups/{id}/settle")
    suspend fun settleDebt(
        @Path("id") groupId: Int,
        @Body request: SettleRequest
    ): Response<MessageResponse>

    // Dashboard
    @GET("/api/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>
}
