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
import pt.isec.amov.cityfix.data.CategoryRepository
import pt.isec.amov.cityfix.data.OccurrenceRepository
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.GeoLocation
import pt.isec.amov.cityfix.model.Occurrence
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusChange
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.Visibility
import pt.isec.amov.cityfix.services.MediaUploadWorker
import pt.isec.amov.cityfix.utils.DistanceUtils

class NewOccurrenceViewModel(
    private val context: Context,
    private val repository: OccurrenceRepository,
    private val categoryRepository: CategoryRepository,
    private val occurrenceId: String? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewOccurrenceUiState())
    val uiState: StateFlow<NewOccurrenceUiState> = _uiState.asStateFlow()

    init {
        val existing = occurrenceId?.let(repository::getOccurrenceById)
        _uiState.value = NewOccurrenceUiState(
            occurrenceId = existing?.id,
            categoryId = existing?.categoryId ?: "",
            situation = existing?.situation ?: Situation.DANO,
            description = existing?.description ?: "",
            range = existing?.range ?: Visibility.PUBLIC,
            imagePaths = existing?.imagePaths ?: emptyList(),
            videoPaths = existing?.videoPaths ?: emptyList(),
            address = existing?.address ?: "",
            latitude = existing?.location?.latitude,
            longitude = existing?.location?.longitude,
            //availableCategories = seedCategories
        )
        loadActiveCategories()
    }

    private fun loadActiveCategories() {
        viewModelScope.launch {
            try {
                val categories = categoryRepository.loadCategories().getActiveSorted()
                _uiState.update { it.copy(availableCategories = categories) }
            } catch (_: Exception) {
                _uiState.update { it.copy(availableCategories = emptyList()) }
            }
        }
    }

    fun onDescriptionChanged(newDescription: String) {
        _uiState.update {
            it.copy(
                description = newDescription,
                descriptionError = if (newDescription.isBlank()) context.getString(R.string.err_description_empty) else null
            )
        }
    }
    fun onCategorySelected(categoryId: String){
        _uiState.update {
            it.copy(
                categoryId = categoryId,
                categoryError = if(categoryId.isBlank()) context.getString(R.string.err_category_empty) else null
            )
        }
    }

    fun onSituationSelected(situation: Situation){
        _uiState.update {
            it.copy(situation = situation)
        }
    }

    fun onVisibilitySelected(range: Visibility){
        _uiState.update {
            it.copy(range = range)
        }
    }

    fun onImagesAdded(paths: List<String>) {
        _uiState.update { it.copy(imagePaths = it.imagePaths + paths) }
    }

    fun onImageRemoved(path: String) {
        _uiState.update { it.copy(imagePaths = it.imagePaths - path) }
    }

    fun onVideoAdded(path: String){
        _uiState.update { it.copy(videoPaths = it.videoPaths + path) }
    }

    fun onVideoRemoved(path: String){
        _uiState.update { it.copy(videoPaths = it.videoPaths - path) }
    }

    fun onAddressChanged(newAddress: String) {
        _uiState.update { it.copy(address = newAddress) }
    }

    fun onLocationSelected(lat: Double, lng: Double, address: String) {
        _uiState.update {
            it.copy(latitude = lat, longitude = lng, address = address)
        }
    }

    fun saveOccurrence(authorId: String = "municipe_01", onSuccess: () -> Unit){
        val current = _uiState.value
        if (!current.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val existing = if (occurrenceId != null) {
                repository.getOccurrenceById(occurrenceId)
            } else {
                null
            }

            val geoLocation = if (current.latitude != null && current.longitude != null) {
                GeoLocation(current.latitude, current.longitude)
            } else {
                null
            }

            val occurrencesList = repository.loadOccurrences()

            // procurar ocorrências parecidas para agrupar:
            // se o utilizador reportar um problema com a mesma tipologia/situação e local aproximado
            // (com um raio de 10mt), será assinalada uma ocorrência de grupo em que vários utilizadores
            // acompanham o mesmo problema de forma agregada.
            val groupMatch = if (existing == null && current.range == Visibility.PUBLIC) {
                occurrencesList.getActive().firstOrNull { activeOcc ->
                    activeOcc.range == Visibility.PUBLIC &&
                            activeOcc.categoryId == current.categoryId &&
                            activeOcc.situation == current.situation &&
                            DistanceUtils.isWithinRadius(
                                activeOcc.location,
                                geoLocation,
                                DistanceUtils.GROUP_RADIUS_METERS // 10m
                            )
                }
            } else {
                null
            }

            val occurrence = if(existing != null){
                // ja existe, vamos atualizar!
                existing.copy(
                    categoryId = current.categoryId,
                    situation = current.situation,
                    description = current.description.trim(),
                    range = current.range,
                    imagePaths = current.imagePaths,
                    videoPaths = current.videoPaths,
                    location = geoLocation,
                    address = current.address.ifBlank { null }
                )
            } else if(groupMatch != null){
                groupMatch.copy(
                    groupOccurrence = true,
                    followersIds = (groupMatch.followersIds + groupMatch.authorId + authorId).distinct()
                    //(O .distinct() evita que o mesmo authorId apareça repetido na lista de seguidores
                )
            } else{
                Occurrence(
                    authorId = authorId,
                    categoryId = current.categoryId,
                    situation = current.situation,
                    description = current.description.trim(),
                    range = current.range,
                    imagePaths = current.imagePaths,
                    videoPaths = current.videoPaths,
                    location = geoLocation,
                    address = current.address.ifBlank { null },
                    statusHistory = listOf(
                        StatusChange(
                            System.currentTimeMillis(),
                            TicketStatus.ABERTO
                        )
                    )
                )
            }
            occurrencesList.upsertOccurrence(occurrence)
            repository.saveOccurrences(occurrencesList)

            val hasPendingLocalMedia = (occurrence.imagePaths + occurrence.videoPaths).any {
                !it.startsWith("http://") && !it.startsWith("https://")
            }
            if (hasPendingLocalMedia) {
                MediaUploadWorker.enqueue(context, occurrence.id)
            }

            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }

}