package com.example.melody.feature.auth.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.melody.core.common.UiState
import com.example.melody.feature.auth.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    onContinueToReset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    val requestState by viewModel.recoveryRequestState.collectAsState()
    val loading = requestState is UiState.Loading
    val requested = requestState is UiState.Success
    val errorMessage = (requestState as? UiState.Error)?.message

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Lupa Password", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Masukkan email akun. Jika alamat tersebut dapat menerima pemulihan, instruksi akan dikirim.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearRecoveryRequestState() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            enabled = !loading && !requested,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        if (requested) {
            Text(
                "Jika email dapat menerima pemulihan, periksa kotak masuk untuk mendapatkan kode OTP.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { onContinueToReset(email.trim()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Masukkan Kode OTP")
            }
        } else {
            Button(
                onClick = { viewModel.requestPasswordRecovery(email) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Text("Mengirim...")
                } else {
                    Text("Kirim Email Pemulihan")
                }
            }
        }
        TextButton(onClick = onBackToLogin, enabled = !loading) {
            Text("Kembali ke Login")
        }
    }
}
