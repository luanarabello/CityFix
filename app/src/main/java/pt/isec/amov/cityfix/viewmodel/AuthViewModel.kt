package pt.isec.amov.cityfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.AuthUser
import pt.isec.amov.cityfix.data.UserRepository
import pt.isec.amov.cityfix.model.GeoLocation
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.utils.Validators

enum class AuthMode {
    SIGN_IN,
    REGISTER,
    PASSWORD_RESET
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val nif: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val nifError: String? = null,
    val locationError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isLoading: Boolean = false,
    val authenticatedUser: AuthUser? = null
)

class AuthViewModel(
    private val context: Context,
    private val repository: AuthRepository,
    private val userRepository: UserRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(authenticatedUser = repository.currentUser)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(name = name, nameError = null, errorMessage = null, infoMessage = null)
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(email = email, emailError = null, errorMessage = null, infoMessage = null)
        }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update {
            it.copy(phone = phone, phoneError = null, errorMessage = null, infoMessage = null)
        }
    }

    fun onNifChanged(nif: String) {
        _uiState.update {
            it.copy(nif = nif, nifError = null, errorMessage = null, infoMessage = null)
        }
    }

    fun onAddressChanged(address: String) {
        _uiState.update {
            it.copy(address = address, errorMessage = null, infoMessage = null)
        }
    }

    fun onLocationSelected(lat: Double, lng: Double, address: String) {
        _uiState.update {
            it.copy(
                latitude = lat,
                longitude = lng,
                address = address,
                locationError = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(password = password, passwordError = null, errorMessage = null)
        }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update {
            it.copy(confirmPassword = password, confirmPasswordError = null, errorMessage = null)
        }
    }

    fun onModeChanged(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                password = "",
                confirmPassword = "",
                nameError = null,
                emailError = null,
                phoneError = null,
                nifError = null,
                locationError = null,
                passwordError = null,
                confirmPasswordError = null,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun submit() {
        if (_uiState.value.isLoading) return

        val current = _uiState.value
        val nameError = if (current.mode == AuthMode.REGISTER && current.name.isBlank()) {
            context.getString(R.string.err_enter_name)
        } else {
            null
        }
        val emailError = if (Validators.isValidEmail(current.email)) null else context.getString(R.string.err_invalid_email)
        val phoneError = if (current.mode == AuthMode.REGISTER && !Validators.isValidPhone(current.phone)) {
            context.getString(R.string.err_invalid_phone)
        } else {
            null
        }
        val nifError = if (current.mode == AuthMode.REGISTER && !Validators.isValidNifFormat(current.nif)) {
            context.getString(R.string.err_invalid_nif)
        } else {
            null
        }
        val locationError = if (
            current.mode == AuthMode.REGISTER && (current.latitude == null || current.longitude == null)
        ) {
            context.getString(R.string.err_select_location)
        } else {
            null
        }
        val passwordError = when {
            current.mode == AuthMode.PASSWORD_RESET -> null
            current.password.isBlank() -> context.getString(R.string.err_enter_password)
            current.mode == AuthMode.REGISTER && current.password.length < MIN_PASSWORD_LENGTH ->
                context.getString(R.string.err_password_min_length, MIN_PASSWORD_LENGTH)
            else -> null
        }
        val confirmPasswordError = if (
            current.mode == AuthMode.REGISTER && current.password != current.confirmPassword
        ) context.getString(R.string.err_passwords_mismatch) else null

        if (nameError != null || emailError != null || phoneError != null || nifError != null ||
            locationError != null || passwordError != null || confirmPasswordError != null
        ) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    phoneError = phoneError,
                    nifError = nifError,
                    locationError = locationError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, infoMessage = null)
            }
            try {
                when (current.mode) {
                    AuthMode.SIGN_IN -> {
                        val user = repository.signIn(current.email, current.password)
                        val cleanEmail = current.email.trim()
                        val isOperatorEmail = cleanEmail.endsWith("@isec.pt", ignoreCase = true)
                        if (!isOperatorEmail && !user.isEmailVerified) {
                            repository.signOut()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = context.getString(R.string.err_verify_email)
                                )
                            }
                            return@launch
                        }
                        val profile = userRepository?.loadUsers()?.findById(user.id)
                        if (profile != null && profile.profileType == UserRole.MUNICIPE && !profile.active) {
                            repository.signOut()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = context.getString(R.string.err_pending_approval)
                                )
                            }
                            return@launch
                        }
                        _uiState.update { it.copy(isLoading = false, authenticatedUser = user) }
                    }

                    AuthMode.REGISTER -> {
                        val existingUsers = userRepository?.loadUsers()
                        if (existingUsers != null && !Validators.isNifUnique(current.nif, existingUsers.rawList())) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    nifError = context.getString(R.string.err_nif_already_registered)
                                )
                            }
                            return@launch
                        }
                        val user = repository.createAccount(current.email, current.password)
                        userRepository?.let { repo ->
                            val usersList = existingUsers ?: repo.loadUsers()
                            val cleanEmail = current.email.trim()
                            val isOperator = cleanEmail.endsWith("@isec.pt", ignoreCase = true)
                            usersList.upsert(
                                User(
                                    id = user.id,
                                    name = current.name.trim(),
                                    email = cleanEmail,
                                    phone = current.phone.trim(),
                                    nif = current.nif.trim(),
                                    location = if (current.latitude != null && current.longitude != null) {
                                        GeoLocation(current.latitude, current.longitude)
                                    } else {
                                        null
                                    },
                                    profileType = if (isOperator) {
                                        UserRole.OPERADOR
                                    } else {
                                        UserRole.MUNICIPE
                                    },
                                    active = isOperator
                                )
                            )
                            repo.saveUsers(usersList)
                        }
                        val isOperator = current.email.trim().endsWith("@isec.pt", ignoreCase = true)
                        if (isOperator) {
                            _uiState.update { it.copy(isLoading = false, authenticatedUser = user) }
                        } else {
                            repository.signOut()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    mode = AuthMode.SIGN_IN,
                                    password = "",
                                    confirmPassword = "",
                                    infoMessage = context.getString(R.string.info_account_created)
                                )
                            }
                        }
                    }

                    AuthMode.PASSWORD_RESET -> {
                        repository.sendPasswordResetEmail(current.email)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                infoMessage = context.getString(R.string.info_recovery_sent)
                            )
                        }
                    }
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = exception.toUserMessage())
                }
            }
        }
    }

    private fun Exception.toUserMessage(): String = when ((this as? FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_EMAIL" -> context.getString(R.string.err_invalid_email)
        "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
            context.getString(R.string.err_auth_incorrect)
        "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.err_auth_email_in_use)
        "ERROR_WEAK_PASSWORD" -> context.getString(R.string.err_auth_weak_password)
        "ERROR_TOO_MANY_REQUESTS" -> context.getString(R.string.err_auth_too_many)
        "ERROR_NETWORK_REQUEST_FAILED" -> context.getString(R.string.err_auth_network)
        else -> context.getString(R.string.err_auth_generic)
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
