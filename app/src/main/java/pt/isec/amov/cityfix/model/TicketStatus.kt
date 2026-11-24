package pt.isec.amov.cityfix.model

import kotlinx.serialization.Serializable

enum class TicketStatus {
    ABERTO, PEDIDO_DE_INFORMACOES, EM_RESOLUCAO, RESOLVIDO, ENCERRADO
}

@Serializable
data class StatusChange(
    val timestamp: Long = 0L,
    val status: TicketStatus = TicketStatus.ABERTO,
    val whyReopen: String? = null // justification
)