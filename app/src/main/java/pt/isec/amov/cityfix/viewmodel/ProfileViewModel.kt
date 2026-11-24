package pt.isec.amov.cityfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.UserRepository
import pt.isec.amov.cityfix.model.GeoLocation
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.StorageUtils
import pt.isec.amov.cityfix.utils.Validators

data class ProfileUiState(
    val userId: String? = null,
    val isRegistration: Boolean = true,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val nif: String = "",
    val location: GeoLocation? = null,
    val profileType: UserRole = UserRole.MUNICIPE,
    val picturePath: String? = null,
    val impactDetection: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val nifError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
            email.isNotBlank() &&
            phone.isNotBlank() &&
            nif.isNotBlank() &&
            nameError == null &&
            emailError == null &&
            phoneError == null &&
            nifError == null
}

class ProfileViewModel(
    private val context: Context,
    private val repository: UserRepository,
    userId: String? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProfileUiState(
            userId = userId,
            isRegistration = userId == null,
            isLoading = true
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var activeUserId: String? = userId
    private var knownUsers: List<User> = emptyList()
    private var persistedPicturePath: String? = null
    private var pendingPicturePath: String? = null

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val usersList = repository.loadUsers()
                knownUsers = usersList.rawList()
                val user = activeUserId?.let(usersList::findById)

                if (activeUserId != null && user == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = context.getString(R.string.err_user_not_found)
                        )
                    }
                    return@launch
                }

                persistedPicturePath = user?.picturePath
                _uiState.value = if (user == null) {
                    ProfileUiState(isRegistration = true)
                } else {
                    ProfileUiState(
                        userId = user.id,
                        isRegistration = false,
                        name = user.name,
                        email = user.email,
                        phone = user.phone,
                        nif = user.nif,
                        location = user.location,
                        profileType = user.profileType,
                        picturePath = user.picturePath,
                        impactDetection = user.impactDetection,
                        nameError = validateName(user.name),
                        emailError = validateEmail(user.email),
                        phoneError = validatePhone(user.phone),
                        nifError = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: context.getString(R.string.err_load_user)
                    )
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = validateName(name),
                errorMessage = null,
                isSaved = false
            )
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = validateEmail(email),
                errorMessage = null,
                isSaved = false
            )
        }
    }

    fun onPhoneChanged(phone: String) {
        _uiState.update {
            it.copy(
                phone = phone,
                phoneError = validatePhone(phone),
                errorMessage = null,
                isSaved = false
            )
        }
    }

    fun onNifChanged(nif: String) {
        _uiState.update {
            it.copy(
                nif = nif,
                nifError = validateNif(nif, activeUserId),
                errorMessage = null,
                isSaved = false
            )
        }
    }

    fun onImpactDetectionChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(impactDetection = enabled, errorMessage = null, isSaved = false)
        }
    }

    fun onLocationSelected(lat: Double, lng: Double) {
        _uiState.update {
            if (it.profileType == UserRole.OPERADOR || it.isRegistration) {
                it.copy(
                    location = GeoLocation(lat, lng),
                    errorMessage = null,
                    isSaved = false
                )
            } else {
                it
            }
        }
    }

    fun onPicturePathChanged(picturePath: String?) {
        if (picturePath == persistedPicturePath) {
            pendingPicturePath?.let(FileUtils::deleteFileFromPath)
            pendingPicturePath = null
        } else {
            pendingPicturePath?.takeIf { it != picturePath }
                ?.let(FileUtils::deleteFileFromPath)
            pendingPicturePath = picturePath
        }

        _uiState.update {
            it.copy(
                picturePath = picturePath,
                errorMessage = null,
                isSaved = false
            )
        }
    }

    fun saveProfile(onSuccess: () -> Unit = {}) {
        val current = _uiState.value
        if (!current.isValid || current.isSaving || current.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val usersList = repository.loadUsers()
                knownUsers = usersList.rawList()

                val nameError = if (activeUserId == null) validateName(current.name) else null
                val emailError = validateEmail(current.email)
                val phoneError = validatePhone(current.phone)
                val nifError = if (activeUserId == null) validateNif(current.nif, activeUserId) else null

                if (nameError != null || emailError != null || phoneError != null || nifError != null) {
                    _uiState.update {
                        it.copy(
                            nameError = nameError,
                            emailError = emailError,
                            phoneError = phoneError,
                            nifError = nifError,
                            isSaving = false
                        )
                    }
                    return@launch
                }

                val existingUser = activeUserId?.let(usersList::findById)
                if (activeUserId != null && existingUser == null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = context.getString(R.string.err_user_not_found)
                        )
                    }
                    return@launch
                }

                val uploadedPicturePath = current.picturePath?.let { path ->
                    StorageUtils.uploadFileIfLocal(path, "profiles")
                }
                val user = (existingUser ?: User()).copy(
                    name = current.name.trim(),
                    email = current.email.trim(),
                    phone = current.phone.trim(),
                    nif = current.nif.trim(),
                    location = current.location,
                    picturePath = uploadedPicturePath,
                    impactDetection = current.impactDetection
                )
                usersList.upsert(user)
                repository.saveUsers(usersList)

                if (existingUser?.picturePath != user.picturePath) {
                    FileUtils.deleteFileFromPath(existingUser?.picturePath)
                }
                activeUserId = user.id
                knownUsers = usersList.rawList()
                persistedPicturePath = user.picturePath
                pendingPicturePath = null
                _uiState.update {
                    it.copy(
                        userId = user.id,
                        isSaving = false,
                        isSaved = true
                    )
                }
                onSuccess()
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = exception.message ?: context.getString(R.string.err_save_user)
                    )
                }
            }
        }
    }

    override fun onCleared() {
        pendingPicturePath?.takeIf { it != persistedPicturePath }
            ?.let(FileUtils::deleteFileFromPath)
        super.onCleared()
    }

    private fun validateName(name: String): String? =
        if (name.isBlank()) context.getString(R.string.err_name_empty) else null

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> context.getString(R.string.err_email_empty)
        !Validators.isValidEmail(email) -> context.getString(R.string.err_invalid_email)
        else -> null
    }

    private fun validatePhone(phone: String): String? = when {
        phone.isBlank() -> context.getString(R.string.err_phone_empty)
        !Validators.isValidPhone(phone) -> context.getString(R.string.err_phone_9_digits)
        else -> null
    }

    private fun validateNif(nif: String, userIdToIgnore: String? = null): String? = when {
        nif.isBlank() -> context.getString(R.string.err_nif_empty)
        !Validators.isValidNifFormat(nif) -> context.getString(R.string.err_nif_9_digits)
        !Validators.isNifUnique(nif, knownUsers, userIdToIgnore) -> context.getString(R.string.err_nif_already_registered)
        else -> null
    }
}
