package idv.wennyli.bloodpressurelog.ui.view.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import idv.wennyli.bloodpressurelog.utils.SnackbarController
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
    snackbarController: SnackbarController,
) : ViewModel() {

    val authState: StateFlow<FirebaseUser?> = authRepository.authStateChanges
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUser)

    /** 轉發 [SnackbarController] 的訊息，供 App 殼層（AppNavigation）統一顯示 Snackbar。 */
    val snackbarMessages: SharedFlow<String> = snackbarController.messages
}
