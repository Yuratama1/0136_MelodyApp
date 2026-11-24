package com.example.melody.feature.auth.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
fun VerifyEmailScreen(
    email: String,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    var token by remember(email) { mutableStateOf("") }
    val verificationState by viewModel.verificationState.collectAsState()
    val loading = verificationState is UiState.Loading
    val errorMessage = (verificationState as? UiState.Error)?.message
    val verified = verificationState is UiState.Success

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Verifikasi email", style = MaterialTheme.typography.headlineMedium)
        Text("Masukkan kode verifikasi yang dikirim ke:", style = MaterialTheme.typography.bodyMedium)
        Text(email, style = MaterialTheme.typography.titleMedium)
        Text("Periksa email Anda dan masukkan kode di bawah ini.", style = MaterialTheme.typography.bodyMedium)

        if (verified) {
            Text(
                "Email berhasil diverifikasi. Kamu dapat melanjutkan ke tahap berikutnya.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            OutlinedTextField(
                value = token,
                onValueChange = { token = it; viewModel.clearVerificationState() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kode atau token verifikasi") },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            )
            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = { viewModel.verifyEmail(email, token) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Verifikasi")
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
