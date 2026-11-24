package pt.isec.amov.cityfix.viewmodel

import pt.isec.amov.cityfix.model.Occurrence
import pt.isec.amov.cityfix.model.TicketStatus
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Values prepared by the presentation layer for the dashboard. */
data class DashboardMetrics(
    val averageRepairTimeMillis: Long?,
    val totalOccurrences: Int,
    val activePercentage: Int,
)

/** Calculates the dashboard values from the current occurrence snapshot. */
fun calculateDashboardMetrics(occurrences: List<Occurrence>): DashboardMetrics {
    val activeStatuses = setOf(
        TicketStatus.ABERTO,
        TicketStatus.PEDIDO_DE_INFORMACOES,
        TicketStatus.EM_RESOLUCAO,
    )
    val activeCount = occurrences.count { it.status in activeStatuses }

    val repairDurations = occurrences.mapNotNull { occurrence ->
        val firstResolvedAt = occurrence.statusHistory
            .asSequence()
            .filter { it.status == TicketStatus.RESOLVIDO }
            .minOfOrNull { it.timestamp }
            ?: return@mapNotNull null

        (firstResolvedAt - occurrence.createdAt).takeIf { it >= 0L }
    }

    val averageRepairTimeMillis = repairDurations
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.roundToLong()

    val activePercentage = if (occurrences.isEmpty()) {
        0
    } else {
        (activeCount * 100.0 / occurrences.size).roundToInt()
    }

    return DashboardMetrics(
        averageRepairTimeMillis = averageRepairTimeMillis,
        totalOccurrences = occurrences.size,
        activePercentage = activePercentage,
    )
}
