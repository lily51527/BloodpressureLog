package idv.wennyli.bloodpressurelog.ui.view.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmailVerificationUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resendCooldownSeconds: Int = 0,
)

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EmailVerificationUiState(email = authRepository.currentUser?.email ?: "")
    )
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private val _navigateToLogin = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToLogin: SharedFlow<Unit> = _navigateToLogin.asSharedFlow()

    init {
        startCooldown()
    }

    fun resendVerificationEmail() {
        if (_uiState.value.resendCooldownSeconds > 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.sendEmailVerification()) {
                is DataState.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    startCooldown()
                }
                is DataState.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is DataState.Loading -> Unit
            }
        }
    }

    private fun startCooldown() {
        viewModelScope.launch {
            for (seconds in RESEND_COOLDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(resendCooldownSeconds = seconds) }
                delay(1_000)
            }
            _uiState.update { it.copy(resendCooldownSeconds = 0) }
        }
    }

    fun backToLogin() {
        viewModelScope.launch {
            authRepository.signOut()
            _navigateToLogin.emit(Unit)
        }
    }

    companion object {
        const val RESEND_COOLDOWN_SECONDS = 60
    }
}
