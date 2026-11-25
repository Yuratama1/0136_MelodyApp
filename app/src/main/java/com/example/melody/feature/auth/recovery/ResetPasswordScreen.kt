package com.example.melody.feature.auth.recovery

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.melody.core.common.UiState
import com.example.melody.feature.auth.AuthViewModel

@Composable
fun ResetPasswordScreen(
    email: String,
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var otp by remember(email) { mutableStateOf("") }
    var newPassword by remember(email) { mutableStateOf("") }
    var confirmPassword by remember(email) { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val resetState by viewModel.passwordResetState.collectAsState()
    val context = LocalContext.current
    val loading = resetState is UiState.Loading
    val errorMessage = (resetState as? UiState.Error)?.message

    LaunchedEffect(resetState) {
        if (resetState is UiState.Success) {
            otp = ""
            newPassword = ""
            confirmPassword = ""
            Toast.makeText(context, "Password berhasil diperbarui.", Toast.LENGTH_SHORT).show()
            viewModel.clearRecoveryState()
            onResetSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Reset Password", style = MaterialTheme.typography.headlineMedium)
        Text("Masukkan kode OTP pemulihan yang dikirim ke $email.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = email,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            readOnly = true,
        )
        OutlinedTextField(
            value = otp,
            onValueChange = { otp = it; viewModel.clearPasswordResetState() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kode OTP pemulihan") },
            singleLine = true,
            enabled = !loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        )
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; viewModel.clearPasswordResetState() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password baru") },
            singleLine = true,
            enabled = !loading,
            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { newPasswordVisible = !newPasswordVisible }, enabled = !loading) {
                    Text(if (newPasswordVisible) "Sembunyikan" else "Tampilkan")
                }
            },
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; viewModel.clearPasswordResetState() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Konfirmasi password baru") },
            singleLine = true,
            enabled = !loading,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }, enabled = !loading) {
                    Text(if (confirmPasswordVisible) "Sembunyikan" else "Tampilkan")
                }
            },
        )
        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = { viewModel.resetPassword(email, otp, newPassword, confirmPassword) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Text("Memperbarui...")
            } else {
                Text("Perbarui Password")
            }
        }
        TextButton(onClick = onBack, enabled = !loading) {
            Text("Kembali")
        }
    }
}
