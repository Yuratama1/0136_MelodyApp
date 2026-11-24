package com.example.melody.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melody.core.common.UiState
import com.example.melody.core.config.SupabaseConfig
import com.example.melody.data.repository.auth.AuthRepository
import com.example.melody.data.repository.auth.AuthRepositoryException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: Provider<AuthRepository>,
    private val supabaseConfig: SupabaseConfig,
) : ViewModel() {
    private val _registrationState = MutableStateFlow<UiState<RegisteredEmail>>(UiState.Empty)
    val registrationState: StateFlow<UiState<RegisteredEmail>> = _registrationState.asStateFlow()

    private val _verificationState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val verificationState: StateFlow<UiState<Unit>> = _verificationState.asStateFlow()

    fun register(displayName: String, email: String, password: String, confirmPassword: String) {
        if (_registrationState.value is UiState.Loading) return
        val validationMessage = RegistrationValidator.validate(displayName, email, password, confirmPassword)
        if (validationMessage != null) {
            _registrationState.value = UiState.Error(validationMessage)
            return
        }
        if (!supabaseConfig.isConfigured) {
            _registrationState.value = UiState.Error(REGISTRATION_UNAVAILABLE)
            return
        }

        _registrationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                authRepository.get().register(displayName.trim(), email.trim(), password)
                _registrationState.value = UiState.Success(RegisteredEmail(email.trim()))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: AuthRepositoryException) {
                _registrationState.value = UiState.Error(exception.message ?: REGISTRATION_ERROR)
            } catch (_: Exception) {
                _registrationState.value = UiState.Error(REGISTRATION_ERROR)
            }
        }
    }

    fun verifyEmail(email: String, token: String) {
        if (_verificationState.value is UiState.Loading) return
        if (email.isBlank() || token.isBlank()) {
            _verificationState.value = UiState.Error(VERIFICATION_FIELDS_REQUIRED)
            return
        }
        if (!supabaseConfig.isConfigured) {
            _verificationState.value = UiState.Error(VERIFICATION_UNAVAILABLE)
            return
        }

        _verificationState.value = UiState.Loading
        viewModelScope.launch {
            try {
                authRepository.get().verifyEmail(email, token.trim())
                _verificationState.value = UiState.Success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: AuthRepositoryException) {
                _verificationState.value = UiState.Error(exception.message ?: VERIFICATION_ERROR)
            } catch (_: Exception) {
                _verificationState.value = UiState.Error(VERIFICATION_ERROR)
            }
        }
    }

    fun clearRegistrationState() {
        _registrationState.value = UiState.Empty
    }

    fun clearVerificationState() {
        _verificationState.value = UiState.Empty
    }

    data class RegisteredEmail(val email: String)

    private companion object {
        const val REGISTRATION_ERROR = "Registrasi gagal. Periksa kembali data Anda dan coba lagi."
        const val VERIFICATION_ERROR = "Verifikasi gagal. Periksa kode dan email Anda, lalu coba lagi."
        const val VERIFICATION_FIELDS_REQUIRED = "Email dan kode verifikasi wajib tersedia."
        const val REGISTRATION_UNAVAILABLE = "Pendaftaran belum tersedia. Coba lagi nanti."
        const val VERIFICATION_UNAVAILABLE = "Verifikasi email belum tersedia. Coba lagi nanti."
    }
}
