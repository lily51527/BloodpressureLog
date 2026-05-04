package idv.wennyli.bloodpressurelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import idv.wennyli.bloodpressurelog.ui.navigation.AppNavigation
import idv.wennyli.bloodpressurelog.ui.theme.BloodPressureLogTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BloodPressureLogTheme {
                AppNavigation(startLoggedIn = authRepository.currentUser?.isEmailVerified == true)
            }
        }
    }
}
