package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.Visibility

data class NewOccurrenceUiState(
    val occurrenceId: String? = null, //criação? null, se não é edição
    val categoryId: String = "",
    val situation: Situation = Situation.DANO,
    val description: String = "",
    val range: Visibility = Visibility.PUBLIC,
    val availableCategories: List<Category> = emptyList(),
    val categoryError: String? = null,
    val descriptionError: String? = null,
    val isSaving: Boolean = false,
    val imagePaths: List<String> = emptyList(),
    val videoPaths: List<String> = emptyList(),
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
){
    val isValid: Boolean
        get() = categoryError == null &&
                descriptionError == null &&
                categoryId.isNotBlank() &&
                description.isNotBlank() &&
                (imagePaths.isNotEmpty() || videoPaths.isNotEmpty()) &&
                latitude != null &&
                longitude != null
}