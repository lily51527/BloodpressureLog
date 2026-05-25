package idv.wennyli.bloodpressurelog.ui.view.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import idv.wennyli.bloodpressurelog.utils.ResourceProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigateToMain = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToMain: SharedFlow<Unit> = _navigateToMain.asSharedFlow()

    private val _navigateToEmailVerification = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToEmailVerification: SharedFlow<Unit> = _navigateToEmailVerification.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun signInWithEmail() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.signInWithEmail(state.email, state.password)
                val currentUser = authRepository.currentUser
                when {
                    currentUser == null -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = resourceProvider.getString(R.string.error_sign_in_failed),
                            )
                        }
                    }
                    currentUser.isEmailVerified -> {
                        _navigateToMain.emit(Unit)
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _navigateToEmailVerification.emit(Unit)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "signInWithEmail failed")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "") }
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.signInAnonymously()
                _navigateToMain.emit(Unit)
            } catch (e: Exception) {
                Timber.e(e, "signInAnonymously failed")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
