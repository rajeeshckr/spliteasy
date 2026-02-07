package com.spliteasy.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spliteasy.app.SplitEasyApp
import com.spliteasy.app.data.TokenManager
import com.spliteasy.app.data.api.ApiClient
import com.spliteasy.app.data.dto.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val tokenManager = TokenManager(SplitEasyApp.appContext)

    fun login(usernameOrEmail: String, password: String) {
        if (usernameOrEmail.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please fill all fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = ApiClient.apiService.login(
                    LoginRequest(usernameOrEmail, password)
                )

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // Save token and user info to DataStore
                    tokenManager.saveAuth(
                        loginResponse.token,
                        loginResponse.userId,
                        loginResponse.username
                    )

                    // Set token in API client
                    ApiClient.setAuthToken(loginResponse.token)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorBody ?: "Login failed"
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
