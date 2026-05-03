package idv.wennyli.bloodpressurelog.ui.view.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import idv.wennyli.bloodpressurelog.BuildConfig
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.ui.theme.BloodPressureLogTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToMain.collect { onLoginSuccess() }
    }

    LoginScreenContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignIn = viewModel::signInWithEmail,
        onAnonymousSignIn = viewModel::signInAnonymously,
        showAnonymousButton = BuildConfig.DEBUG,
    )
}

@Composable
internal fun LoginScreenContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onAnonymousSignIn: () -> Unit,
    showAnonymousButton: Boolean,
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
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.label_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.label_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSignIn() }),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.button_login))
                }
            }

            if (showAnonymousButton) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAnonymousSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                ) {
                    Text(stringResource(R.string.button_anonymous_login_debug))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Login - Default")
@Composable
private fun LoginScreenPreview() {
    BloodPressureLogTheme {
        LoginScreenContent(
            uiState = LoginUiState(email = "user@example.com", password = "password"),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onAnonymousSignIn = {},
            showAnonymousButton = true,
        )
    }
}

@Preview(showBackground = true, name = "Login - Loading")
@Composable
private fun LoginScreenLoadingPreview() {
    BloodPressureLogTheme {
        LoginScreenContent(
            uiState = LoginUiState(isLoading = true),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onAnonymousSignIn = {},
            showAnonymousButton = true,
        )
    }
}

@Preview(showBackground = true, name = "Login - Error")
@Composable
private fun LoginScreenErrorPreview() {
    BloodPressureLogTheme {
        LoginScreenContent(
            uiState = LoginUiState(
                email = "user@example.com",
                errorMessage = "Invalid email or password.",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onAnonymousSignIn = {},
            showAnonymousButton = false,
        )
    }
}
