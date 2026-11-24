package pt.isec.amov.cityfix.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Occurrence
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.Visibility
import pt.isec.amov.cityfix.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OccurrenceCard(
    occurrence: Occurrence,
    categoryName: String = occurrence.categoryId,
    isExpanded: Boolean = false,
    showStatus: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val firstImage = occurrence.imagePaths.firstOrNull()
    val accentColor = if (showStatus) {
        statusColors(occurrence.status).second
    } else {
        MaterialTheme.colorScheme.primary
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (firstImage != null) {
                        AsyncImage(
                            model = ImageUtils.getThumbnailOrOriginal(context, firstImage),
                            contentDescription = "Occurrence thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = situationIcon(occurrence.situation),
                                contentDescription = occurrence.situation.displayLabel(),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = occurrence.situation.displayLabel(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (showStatus) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VisibilityBadge(range = occurrence.range)
                        StatusBadge(status = occurrence.status)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = occurrence.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) 4 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = dateFormat.format(Date(occurrence.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.weight(1f))

                    if (occurrence.groupOccurrence) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.group_issue_badge, occurrence.followersIds.size),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: TicketStatus,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = statusColors(status)
    BadgePill(
        label = status.displayLabel(),
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier
    )
}

@Composable
fun VisibilityBadge(
    range: Visibility,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = visibilityColors(range)
    BadgePill(
        label = range.displayLabel(),
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier
    )
}

@Composable
private fun BadgePill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun visibilityColors(range: Visibility): Pair<Color, Color> = when (range) {
    Visibility.PUBLIC -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
    Visibility.PRIVATE -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
}

private fun statusColors(status: TicketStatus): Pair<Color, Color> = when (status) {
    TicketStatus.ABERTO -> Color(0xFFDCEAFE) to Color(0xFF1D4ED8)
    TicketStatus.PEDIDO_DE_INFORMACOES -> Color(0xFFFEF3C7) to Color(0xFFB45309)
    TicketStatus.EM_RESOLUCAO -> Color(0xFFE0E7FF) to Color(0xFF4338CA)
    TicketStatus.RESOLVIDO -> Color(0xFFDCFCE7) to Color(0xFF15803D)
    TicketStatus.ENCERRADO -> Color(0xFFE5E7EB) to Color(0xFF374151)
}

fun situationIcon(situation: Situation): ImageVector = when (situation) {
    Situation.DANO -> Icons.Default.ReportProblem
    Situation.AVARIA -> Icons.Default.Build
    Situation.DESTRUICAO -> Icons.Default.DeleteForever
    Situation.ROUBO -> Icons.Default.Warning
}
