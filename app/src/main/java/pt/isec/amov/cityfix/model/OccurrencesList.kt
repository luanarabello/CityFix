package pt.isec.amov.cityfix.model

class OccurrencesList(initialOccurences: List<Occurrence> = emptyList()) {
    private val _occurrences = initialOccurences.toMutableList()

    fun getSortedByDate(): List<Occurrence> {
        return _occurrences.sortedByDescending { it.createdAt }
    }

    fun getActive(): List<Occurrence>{
        return _occurrences
            .filter { it.status != TicketStatus.ENCERRADO && it.status != TicketStatus.RESOLVIDO }
            .sortedByDescending { it.createdAt }
    }

    fun getPublicActive(): List<Occurrence>{
        return _occurrences
            .filter { it.range == Visibility.PUBLIC && it.status != TicketStatus.ENCERRADO && it.status != TicketStatus.RESOLVIDO }
            .sortedByDescending { it.createdAt }
    }

    fun getByAuthor(authorId: String): List<Occurrence> {
        return _occurrences
            .filter { it.authorId == authorId }
            .sortedByDescending { it.createdAt }
    }

    fun findById(id: String): Occurrence? = _occurrences.find { it.id == id }

    fun upsertOccurrence(occurrence: Occurrence) {
        val index = _occurrences.indexOfFirst { it.id == occurrence.id }
        if (index >= 0) {
            _occurrences[index] = occurrence
        } else {
            _occurrences.add(occurrence)
        }
    }
    fun rawList(): List<Occurrence> = _occurrences.toList() //Immutable snapshot copy of occurences for safe data persistence

    fun resolveOccurrence(
        id: String,
        resolutionImages: List<String>,
        resolutionVideos: List<String>
    ): Boolean{
        val existing = findById(id) ?: return false
        if(existing.status == TicketStatus.ENCERRADO) return false
        if(resolutionImages.isEmpty() && resolutionVideos.isEmpty()) return false

        val updated = existing.copy(
            status = TicketStatus.RESOLVIDO,
            resolutionImagePaths = existing.resolutionImagePaths + resolutionImages,
            resolutionVideoPaths = existing.resolutionVideoPaths + resolutionVideos,
            statusHistory = existing.statusHistory + StatusChange(System.currentTimeMillis(), TicketStatus.RESOLVIDO)
        )

        upsertOccurrence(updated)
        return true
    }

    fun canReopen(occurrence: Occurrence, nowMillis: Long = System.currentTimeMillis()): Boolean{
        if(occurrence.status != TicketStatus.RESOLVIDO) return false

        val resolvedAt = occurrence.statusHistory
            .lastOrNull { it.status == TicketStatus.RESOLVIDO } // busca a ultima vez que o status do ticket foi RESOLVIDO
            ?.timestamp ?: return false // se existe, devolve o timestamp, se nao, acaba a função aí

        val fiveDaysMillis = 5L * 24 * 60 * 60 * 1000 // multiplicação em Long

        return (nowMillis - resolvedAt) <= fiveDaysMillis
    }

    fun updateStatus(id: String, newStatus: TicketStatus, whyReopen: String? = null): Boolean{
        val existing = findById(id) ?: return false
        if (existing.status == TicketStatus.ENCERRADO) return false

        val updated = existing.copy(
            status = newStatus,
            statusHistory = existing.statusHistory + StatusChange(
                timestamp = System.currentTimeMillis(),
                status = newStatus,
                whyReopen = whyReopen?.trim()?.takeIf { it.isNotEmpty() }
            )
        )

        upsertOccurrence(updated)
        return true
    }

    fun closeExpiredResolved(nowMillis: Long = System.currentTimeMillis()): Boolean {
        var anyClosed = false
        _occurrences
            .filter { it.status == TicketStatus.RESOLVIDO && !canReopen(it, nowMillis) }
            .forEach { occ ->
                if (updateStatus(occ.id, TicketStatus.ENCERRADO)) {
                    anyClosed = true
                }
            }
        return anyClosed
    }
}
