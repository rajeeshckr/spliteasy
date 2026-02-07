package com.spliteasy.app.presentation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spliteasy.app.data.api.ApiClient
import com.spliteasy.app.data.dto.BalanceResponse
import com.spliteasy.app.data.dto.ExpenseResponse
import com.spliteasy.app.data.dto.GroupDetailResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val group: GroupDetailResponse? = null,
    val expenses: List<ExpenseResponse> = emptyList(),
    val balances: BalanceResponse? = null
)

class GroupDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    fun loadGroup(groupId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = ApiClient.apiService.getGroup(groupId)

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        group = response.body()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load group"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }

    fun loadExpenses(groupId: Int) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getExpenses(groupId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(expenses = response.body()!!)
                }
            } catch (e: Exception) {
                // Silently fail for expenses
            }
        }
    }

    fun loadBalances(groupId: Int) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getBalances(groupId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(balances = response.body())
                }
            } catch (e: Exception) {
                // Silently fail for balances
            }
        }
    }

    fun settleDebt(groupId: Int, fromUserId: Int, toUserId: Int, amountCents: Long) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.settleDebt(
                    groupId,
                    com.spliteasy.app.data.dto.SettleRequest(fromUserId, toUserId, amountCents)
                )
                if (response.isSuccessful) {
                    // Reload balances to show updated state
                    loadBalances(groupId)
                }
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }
}
