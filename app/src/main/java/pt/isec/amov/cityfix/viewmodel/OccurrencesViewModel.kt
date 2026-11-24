package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.OccurrenceRepository
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Occurrence
import pt.isec.amov.cityfix.model.OccurrencesList
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.utils.StorageUtils

class OccurrencesViewModel(private val repository: OccurrenceRepository) : ViewModel() {
    private val _occurrencesList = MutableStateFlow<List<Occurrence>>(emptyList())
    val occurrencesList: StateFlow<List<Occurrence>> = _occurrencesList.asStateFlow()

    private val _dashboardMetrics = MutableStateFlow(
        DashboardMetrics(
            averageRepairTimeMillis = null,
            totalOccurrences = 0,
            activePercentage = 0,
        )
    )
    val dashboardMetrics: StateFlow<DashboardMetrics> = _dashboardMetrics.asStateFlow()

    private val _messagesByOccurrence = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messagesByOccurrence: StateFlow<Map<String, List<Message>>> =
        _messagesByOccurrence.asStateFlow()

    private var currentOccurrencesList = OccurrencesList()

    init {
        refreshOccurrences()
        viewModelScope.launch {
            repository.observeOccurrences().collect { remoteList ->
                currentOccurrencesList = OccurrencesList(remoteList)
                publishOccurrences()
            }
        }
        viewModelScope.launch {
            repository.observeMessages().collect { grouped ->
                _messagesByOccurrence.value = grouped
            }
        }
    }

    fun refreshOccurrences() {
        viewModelScope.launch {
            currentOccurrencesList = repository.loadOccurrences()
            publishOccurrences()
        }
    }

    fun checkExpiredResolved() {
        viewModelScope.launch {
            if (currentOccurrencesList.closeExpiredResolved()) {
                repository.saveOccurrences(currentOccurrencesList)
                publishOccurrences()
            }
        }
    }

    fun canReopen(occurrence: Occurrence): Boolean = currentOccurrencesList.canReopen(occurrence)

    fun sendMessage(
        occurrenceId: String,
        senderId: String,
        senderRole: UserRole,
        text: String
    ) {
        val messageText = text.trim()
        if (messageText.isEmpty()) return

        val message = Message(
            occurrenceId = occurrenceId,
            senderId = senderId,
            senderRole = senderRole,
            text = messageText
        )
        _messagesByOccurrence.update { currentMessages ->
            currentMessages + (occurrenceId to (currentMessages[occurrenceId].orEmpty() + message))
        }
        repository.saveMessage(message)
    }

    fun changeStatus(
        occurrenceId: String,
        newStatus: TicketStatus,
        whyReopen: String? = null
    ) {
        viewModelScope.launch {
            val changed = currentOccurrencesList.updateStatus(occurrenceId, newStatus, whyReopen)
            if (changed) {
                repository.saveOccurrences(currentOccurrencesList)
                publishOccurrences()
            }
        }
    }

    fun reopenOccurrence(occurrenceId: String, justification: String){
        val existing = currentOccurrencesList.findById(occurrenceId) ?: return
        if (justification.isBlank() || !currentOccurrencesList.canReopen(existing)) return

        changeStatus(occurrenceId, TicketStatus.ABERTO, justification)
    }

    fun resolveOccurrence(
        occurrenceId: String,
        resolutionImages: List<String>,
        resolutionVideos: List<String>
    ){
        viewModelScope.launch {
            val uploadedImages = StorageUtils.uploadFilesIfLocal(
                resolutionImages,
                "occurrences/resolutions/images"
            )
            val uploadedVideos = StorageUtils.uploadFilesIfLocal(
                resolutionVideos,
                "occurrences/resolutions/videos"
            )
            val resolved = currentOccurrencesList.resolveOccurrence(
                occurrenceId,
                uploadedImages,
                uploadedVideos
            )
            if(resolved){
                repository.saveOccurrences(currentOccurrencesList)
                publishOccurrences()
            }
        }
    }

    fun getOccurrenceById(id: String): Occurrence? = currentOccurrencesList.findById(id)

    private fun publishOccurrences() {
        val snapshot = currentOccurrencesList.getSortedByDate()
        _occurrencesList.value = snapshot
        _dashboardMetrics.value = calculateDashboardMetrics(snapshot)
    }
}
