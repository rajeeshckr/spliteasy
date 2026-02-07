package com.spliteasy.app.presentation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spliteasy.app.data.api.ApiClient
import com.spliteasy.app.data.dto.CreateGroupRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateGroupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdGroupId: Int? = null
)

class CreateGroupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    fun createGroup(name: String, description: String?) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Group name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val response = ApiClient.apiService.createGroup(
                    CreateGroupRequest(name, description)
                )

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        createdGroupId = response.body()!!.id
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to create group"
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
}
