package com.example.melody.feature.auth.register

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.melody.core.common.UiState
import com.example.melody.feature.auth.AuthViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onVerificationRequired: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val registrationState by viewModel.registrationState.collectAsState()
    LaunchedEffect(registrationState) {
        val success = registrationState as? UiState.Success ?: return@LaunchedEffect
        viewModel.clearRegistrationState()
        onVerificationRequired(success.data.email)
    }

    val loading = registrationState is UiState.Loading
    val errorMessage = (registrationState as? UiState.Error)?.message

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Buat akun Melody", style = MaterialTheme.typography.headlineMedium)
        Text("Daftar untuk mulai menggunakan Melody.", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it; viewModel.clearRegistrationState() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nama tampilan") },
            singleLine = true,
            enabled = !loading,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; viewModel.clearRegistrationState() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            enabled = !loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearRegistrationState() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            enabled = !loading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }, enabled = !loading) {
                    Text(if (passwordVisible) "Sembunyikan" else "Tampilkan")
                }
            },
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; viewModel.clearRegistrationState() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Konfirmasi password") },
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
            onClick = { viewModel.register(displayName, email, password, confirmPassword) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Text("Mendaftar...")
            } else {
                Text("Daftar")
            }
        }
    }
}
