package pt.isec.amov.cityfix.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pt.isec.amov.cityfix.model.Occurrence
import pt.isec.amov.cityfix.model.StatusChange
import pt.isec.amov.cityfix.model.TicketStatus

class DashboardMetricsTest {

    @Test
    fun emptyList_returnsZeroMetricsAndNullAverage() {
        val metrics = calculateDashboardMetrics(emptyList())

        assertNull(metrics.averageRepairTimeMillis)
        assertEquals(0, metrics.totalOccurrences)
        assertEquals(0, metrics.activePercentage)
    }

    @Test
    fun withoutResolvedOccurrences_returnsNullAverageAndCorrectActivePercentage() {
        val occurrences = listOf(
            Occurrence(id = "1", status = TicketStatus.ABERTO),
            Occurrence(id = "2", status = TicketStatus.PEDIDO_DE_INFORMACOES),
            Occurrence(id = "3", status = TicketStatus.EM_RESOLUCAO),
            Occurrence(id = "4", status = TicketStatus.ENCERRADO),
        )

        val metrics = calculateDashboardMetrics(occurrences)

        assertNull(metrics.averageRepairTimeMillis)
        assertEquals(4, metrics.totalOccurrences)
        assertEquals(75, metrics.activePercentage)
    }

    @Test
    fun reopenedOccurrence_usesFirstResolvedTransition() {
        val occurrence = Occurrence(
            id = "1",
            createdAt = 1_000L,
            status = TicketStatus.RESOLVIDO,
            statusHistory = listOf(
                StatusChange(timestamp = 1_000L, status = TicketStatus.ABERTO),
                StatusChange(timestamp = 5_000L, status = TicketStatus.RESOLVIDO),
                StatusChange(timestamp = 6_000L, status = TicketStatus.ABERTO),
                StatusChange(timestamp = 10_000L, status = TicketStatus.RESOLVIDO),
            ),
        )

        val metrics = calculateDashboardMetrics(listOf(occurrence))

        assertEquals(4_000L, metrics.averageRepairTimeMillis)
        assertEquals(1, metrics.totalOccurrences)
        assertEquals(0, metrics.activePercentage)
    }

    @Test
    fun mixedOccurrences_averagesOnlyResolvedAndRoundsActivePercentage() {
        val occurrences = listOf(
            Occurrence(
                id = "1",
                createdAt = 1_000L,
                status = TicketStatus.RESOLVIDO,
                statusHistory = listOf(
                    StatusChange(timestamp = 3_000L, status = TicketStatus.RESOLVIDO),
                ),
            ),
            Occurrence(
                id = "2",
                createdAt = 2_000L,
                status = TicketStatus.ENCERRADO,
                statusHistory = listOf(
                    StatusChange(timestamp = 6_000L, status = TicketStatus.RESOLVIDO),
                    StatusChange(timestamp = 9_000L, status = TicketStatus.ENCERRADO),
                ),
            ),
            Occurrence(
                id = "3",
                createdAt = 5_000L,
                status = TicketStatus.ABERTO,
            ),
        )

        val metrics = calculateDashboardMetrics(occurrences)

        assertEquals(3_000L, metrics.averageRepairTimeMillis)
        assertEquals(3, metrics.totalOccurrences)
        assertEquals(33, metrics.activePercentage)
    }
}
