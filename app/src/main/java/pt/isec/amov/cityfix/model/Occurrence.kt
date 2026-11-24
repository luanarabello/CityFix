package pt.isec.amov.cityfix.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class Situation{
    DANO, AVARIA, DESTRUICAO, ROUBO
}

enum class Visibility{
    PUBLIC, PRIVATE
}

@Serializable
data class Occurrence(
    val id: String = UUID.randomUUID().toString(),
    val authorId: String = "",
    val categoryId: String = "", //guardo o id porque a categoria em si pode mudar
    val situation: Situation = Situation.DANO,
    val description: String = "",
    val imagePaths:  List<String> = emptyList(),
    val videoPaths:  List<String> = emptyList(),
    val range: Visibility = Visibility.PUBLIC,
    val createdAt: Long = System.currentTimeMillis(),
    val status: TicketStatus = TicketStatus.ABERTO,
    val statusHistory: List<StatusChange> = emptyList(),
    val resolutionImagePaths: List<String> = emptyList(),
    val resolutionVideoPaths: List<String> = emptyList(),
    val location: GeoLocation? = null,
    val address: String? = null,
    val groupOccurrence: Boolean = false,
    val followersIds: List<String> = emptyList()
)

