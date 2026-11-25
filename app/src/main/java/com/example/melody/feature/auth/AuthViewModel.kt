package com.example.melody.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melody.core.common.UiState
import com.example.melody.core.config.SupabaseConfig
import com.example.melody.data.repository.auth.AuthRepository
import com.example.melody.data.repository.auth.AuthRepositoryException
import com.example.melody.data.session.AuthSessionManager
import com.example.melody.data.session.AuthState
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
    sessionManagerProvider: Provider<AuthSessionManager>,
    private val supabaseConfig: SupabaseConfig,
) : ViewModel() {
    private val sessionManager = sessionManagerProvider.get()

    val authState: StateFlow<AuthState> = sessionManager.authState

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    private val _registrationState = MutableStateFlow<UiState<RegisteredEmail>>(UiState.Empty)
    val registrationState: StateFlow<UiState<RegisteredEmail>> = _registrationState.asStateFlow()

    private val _verificationState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val verificationState: StateFlow<UiState<Unit>> = _verificationState.asStateFlow()

    private val _recoveryRequestState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val recoveryRequestState: StateFlow<UiState<Unit>> = _recoveryRequestState.asStateFlow()

    private val _passwordResetState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val passwordResetState: StateFlow<UiState<Unit>> = _passwordResetState.asStateFlow()

    init {
        viewModelScope.launch { sessionManager.restoreSession() }
    }

    fun login(email: String, password: String) {
        if (_loginState.value is UiState.Loading) return
        val validationMessage = LoginValidator.validate(email, password)
        if (validationMessage != null) {
            _loginState.value = UiState.Error(validationMessage)
            return
        }
        if (!supabaseConfig.isConfigured) {
            _loginState.value = UiState.Error(LOGIN_UNAVAILABLE)
            return
        }

        _loginState.value = UiState.Loading
        viewModelScope.launch {
            try {
                authRepository.get().login(email.trim(), password)
                _loginState.value = UiState.Success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: AuthRepositoryException) {
                _loginState.value = UiState.Error(exception.message ?: LOGIN_ERROR)
            } catch (_: Exception) {
                _loginState.value = UiState.Error(LOGIN_ERROR)
            }
        }
    }

    fun clearLoginState() {
        if (_loginState.value !is UiState.Loading) _loginState.value = UiState.Empty
    }

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

    fun requestPasswordRecovery(email: String) {
        if (_recoveryRequestState.value is UiState.Loading) return
        val validationMessage = PasswordRecoveryValidator.validateEmail(email)
        if (validationMessage != null) {
            _recoveryRequestState.value = UiState.Error(validationMessage)
            return
        }
        if (!supabaseConfig.isConfigured) {
            _recoveryRequestState.value = UiState.Error(RECOVERY_UNAVAILABLE)
            return
        }

        _recoveryRequestState.value = UiState.Loading
        viewModelScope.launch {
            try {
                authRepository.get().requestPasswordRecovery(email.trim())
                _recoveryRequestState.value = UiState.Success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: AuthRepositoryException) {
                _recoveryRequestState.value = UiState.Error(exception.message ?: RECOVERY_REQUEST_ERROR)
            } catch (_: Exception) {
                _recoveryRequestState.value = UiState.Error(RECOVERY_REQUEST_ERROR)
            }
        }
    }

    fun resetPassword(email: String, otp: String, password: String, confirmPassword: String) {
        if (_passwordResetState.value is UiState.Loading) return
        val validationMessage = PasswordRecoveryValidator.validateReset(email, otp, password, confirmPassword)
        if (validationMessage != null) {
            _passwordResetState.value = UiState.Error(validationMessage)
            return
        }
        if (!supabaseConfig.isConfigured) {
            _passwordResetState.value = UiState.Error(RESET_UNAVAILABLE)
            return
        }

        _passwordResetState.value = UiState.Loading
        viewModelScope.launch {
            try {
                authRepository.get().resetPassword(email.trim(), otp.trim(), password)
                _passwordResetState.value = UiState.Success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: AuthRepositoryException) {
                _passwordResetState.value = UiState.Error(exception.message ?: PASSWORD_RESET_ERROR)
            } catch (_: Exception) {
                _passwordResetState.value = UiState.Error(PASSWORD_RESET_ERROR)
            }
        }
    }

    fun clearRecoveryRequestState() {
        if (_recoveryRequestState.value !is UiState.Loading) _recoveryRequestState.value = UiState.Empty
    }

    fun clearPasswordResetState() {
        if (_passwordResetState.value !is UiState.Loading) _passwordResetState.value = UiState.Empty
    }

    fun clearRecoveryState() {
        clearRecoveryRequestState()
        clearPasswordResetState()
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
        const val LOGIN_UNAVAILABLE = "Login belum tersedia. Coba lagi nanti."
        const val LOGIN_ERROR = "Login gagal. Periksa data Anda dan coba lagi."
        const val RECOVERY_UNAVAILABLE = "Pemulihan password belum tersedia. Coba lagi nanti."
        const val RESET_UNAVAILABLE = "Reset password belum tersedia. Coba lagi nanti."
        const val RECOVERY_REQUEST_ERROR = "Permintaan pemulihan belum dapat diproses. Coba lagi nanti."
        const val PASSWORD_RESET_ERROR = "Password baru tidak dapat diperbarui. Coba lagi."
    }
}
