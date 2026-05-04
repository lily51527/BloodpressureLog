package idv.wennyli.bloodpressurelog.ui.view.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.ui.theme.BloodPressureLogTheme

@Composable
fun EmailVerificationScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToLogin.collect { onNavigateToLogin() }
    }

    EmailVerificationScreenContent(
        uiState = uiState,
        onResend = viewModel::resendVerificationEmail,
        onBackToLogin = viewModel::backToLogin,
    )
}

@Composable
internal fun EmailVerificationScreenContent(
    uiState: EmailVerificationUiState,
    onResend: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.screen_title_email_verification),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.email_verification_message, uiState.email),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onResend,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.resendCooldownSeconds == 0,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else if (uiState.resendCooldownSeconds > 0) {
                    Text(stringResource(R.string.button_resend_cooldown, uiState.resendCooldownSeconds))
                } else {
                    Text(stringResource(R.string.button_resend_verification_email))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onBackToLogin) {
                Text(stringResource(R.string.button_back_to_login))
            }
        }
    }
}

@Preview(showBackground = true, name = "EmailVerification - Default")
@Composable
private fun EmailVerificationScreenPreview() {
    BloodPressureLogTheme {
        EmailVerificationScreenContent(
            uiState = EmailVerificationUiState(email = "user@example.com"),
            onResend = {},
            onBackToLogin = {},
        )
    }
}

@Preview(showBackground = true, name = "EmailVerification - Cooldown")
@Composable
private fun EmailVerificationScreenCooldownPreview() {
    BloodPressureLogTheme {
        EmailVerificationScreenContent(
            uiState = EmailVerificationUiState(email = "user@example.com", resendCooldownSeconds = 42),
            onResend = {},
            onBackToLogin = {},
        )
    }
}
