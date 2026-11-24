package pt.isec.amov.cityfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.CategoryRepository
import pt.isec.amov.cityfix.data.UserRepository
import pt.isec.amov.cityfix.model.CategoriesList
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.model.UsersList
import java.text.Collator
import java.util.Locale

data class OperatorCategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val mutationVersion: Int = 0
)

data class CitizenValidationUiState(
    val pendingCitizens: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val approvingUserIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class OperatorViewModel(
    private val context: Context,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CitizenValidationUiState())
    val uiState: StateFlow<CitizenValidationUiState> = _uiState.asStateFlow()

    private val _categoriesUiState = MutableStateFlow(OperatorCategoriesUiState())
    val categoriesUiState: StateFlow<OperatorCategoriesUiState> = _categoriesUiState.asStateFlow()

    init {
        refreshPendingCitizens()
        refreshCategories()
    }

    fun refreshPendingCitizens() {
        _uiState.update {
            it.copy(isLoading = true, errorMessage = null)
        }
        viewModelScope.launch {
            try {
                val usersList = userRepository.loadUsers()
                _uiState.update {
                    it.copy(
                        pendingCitizens = getPendingCitizens(usersList),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: context.getString(R.string.err_load_pending_citizens)
                    )
                }
            }
        }
    }

    fun approveCitizen(userId: String) {
        if (userId in _uiState.value.approvingUserIds) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    approvingUserIds = it.approvingUserIds + userId,
                    errorMessage = null
                )
            }

            try {
                val usersList = userRepository.loadUsers()
                val citizen = usersList.findById(userId)
                if (citizen == null || citizen.profileType != UserRole.MUNICIPE) {
                    _uiState.update {
                        it.copy(
                            approvingUserIds = it.approvingUserIds - userId,
                            errorMessage = context.getString(R.string.err_pending_citizen_not_found)
                        )
                    }
                    return@launch
                }

                if (!citizen.active) {
                    usersList.upsert(citizen.copy(active = true))
                    userRepository.saveUsers(usersList)
                }

                _uiState.update {
                    it.copy(
                        pendingCitizens = getPendingCitizens(usersList),
                        approvingUserIds = it.approvingUserIds - userId,
                        errorMessage = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        approvingUserIds = it.approvingUserIds - userId,
                        errorMessage = exception.message ?: context.getString(R.string.err_approve_citizen)
                    )
                }
            }
        }
    }


    fun refreshCategories() {
        _categoriesUiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val categories = categoryRepository.loadCategories()
                _categoriesUiState.update {
                    it.copy(
                        categories = categories.getAllSorted(),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (exception: Exception) {
                _categoriesUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: context.getString(R.string.err_load_categories)
                    )
                }
            }
        }
    }

    fun saveCategory(categoryId: String?, title: String) {
        if (_categoriesUiState.value.isSaving) return

        val cleanTitle = title.trim()
        val initialError = validateCategoryTitle(cleanTitle, categoryId, _categoriesUiState.value.categories)
        if (initialError != null) {
            _categoriesUiState.update { it.copy(errorMessage = initialError) }
            return
        }

        viewModelScope.launch {
            _categoriesUiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val loadedCategories = categoryRepository.loadCategories()
                val categories = CategoriesList(loadedCategories.rawList())
                val currentCategory = categoryId?.let(categories::findById)
                if (categoryId != null && currentCategory == null) {
                    _categoriesUiState.update {
                        it.copy(isSaving = false, errorMessage = context.getString(R.string.err_category_not_found))
                    }
                    return@launch
                }

                val currentError = validateCategoryTitle(cleanTitle, categoryId, categories.getAllSorted())
                if (currentError != null) {
                    _categoriesUiState.update { it.copy(isSaving = false, errorMessage = currentError) }
                    return@launch
                }

                val updatedCategory = currentCategory?.copy(title = cleanTitle)
                    ?: Category(title = cleanTitle)
                categories.upsert(updatedCategory)
                categoryRepository.saveCategories(categories)
                _categoriesUiState.update {
                    it.copy(
                        categories = categories.getAllSorted(),
                        isSaving = false,
                        errorMessage = null,
                        mutationVersion = it.mutationVersion + 1
                    )
                }
            } catch (exception: Exception) {
                _categoriesUiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = exception.message ?: context.getString(R.string.err_save_category)
                    )
                }
            }
        }
    }

    fun setCategoryActive(categoryId: String, active: Boolean) {
        if (_categoriesUiState.value.isSaving) return

        viewModelScope.launch {
            _categoriesUiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val loadedCategories = categoryRepository.loadCategories()
                val categories = CategoriesList(loadedCategories.rawList())
                val category = categories.findById(categoryId)
                if (category == null) {
                    _categoriesUiState.update {
                        it.copy(isSaving = false, errorMessage = context.getString(R.string.err_category_not_found))
                    }
                    return@launch
                }

                categories.upsert(category.copy(active = active))
                categoryRepository.saveCategories(categories)
                _categoriesUiState.update {
                    it.copy(
                        categories = categories.getAllSorted(),
                        isSaving = false,
                        errorMessage = null,
                        mutationVersion = it.mutationVersion + 1
                    )
                }
            } catch (exception: Exception) {
                _categoriesUiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = exception.message ?: context.getString(R.string.err_update_category)
                    )
                }
            }
        }
    }

    private fun validateCategoryTitle(
        title: String,
        categoryIdToIgnore: String?,
        categories: List<Category>
    ): String? {
        if (title.isBlank()) return context.getString(R.string.err_category_name_empty)
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
        val alreadyExists = categories.any { category ->
            category.id != categoryIdToIgnore && collator.compare(category.title.trim(), title) == 0
        }
        return if (alreadyExists) context.getString(R.string.err_category_duplicate) else null
    }

    private fun getPendingCitizens(usersList: UsersList): List<User> =
        usersList.getAllSorted().filter {
            it.profileType == UserRole.MUNICIPE && !it.active
        }
}

class OperatorViewModelFactory(
    private val context: Context,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OperatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OperatorViewModel(context.applicationContext, userRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
