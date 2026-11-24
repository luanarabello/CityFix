package pt.isec.amov.cityfix.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Occurrence
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.Visibility
import pt.isec.amov.cityfix.ui.components.OccurrenceMessagesSection
import pt.isec.amov.cityfix.ui.components.StatusBadge
import pt.isec.amov.cityfix.ui.components.VideoThumbnailItem
import pt.isec.amov.cityfix.ui.components.displayLabel
import pt.isec.amov.cityfix.ui.components.situationIcon
import pt.isec.amov.cityfix.ui.navigation.Profile
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OccurrenceDetailScreen(
    occurrence: Occurrence?,
    currentProfile: Profile,
    categoryName: String = occurrence?.categoryId ?: "",
    author: User? = null,
    canReopen: Boolean = false,
    onChangeStatus: (TicketStatus) -> Unit = {},
    onReopen: (String) -> Unit = {},
    onResolve: (List<String>, List<String>) -> Unit = { _, _ -> },
    messages: List<Message> = emptyList(),
    onSendMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (occurrence == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.occurrence_not_found),
                style = MaterialTheme.typography.titleMedium
            )
        }
        return
    }

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val createdAtFormatted = dateFormat.format(Date(occurrence.createdAt))

    var showReopenDialog by rememberSaveable(occurrence.id) { mutableStateOf(false) }
    var justification by rememberSaveable(occurrence.id) { mutableStateOf("") }

    var showInfoDialog by rememberSaveable(occurrence.id) { mutableStateOf(false) }
    var infoMessageText by rememberSaveable(occurrence.id) { mutableStateOf("") }
    var infoTargetStatus by rememberSaveable(occurrence.id) {
        mutableStateOf(TicketStatus.PEDIDO_DE_INFORMACOES)
    }

    var showResolveDialog by rememberSaveable(occurrence.id) { mutableStateOf(false) }
    var resolutionImages by rememberSaveable(occurrence.id) { mutableStateOf<List<String>>(emptyList()) }
    var resolutionVideos by rememberSaveable(occurrence.id) { mutableStateOf<List<String>>(emptyList()) }
    var tempPhotoPath by rememberSaveable(occurrence.id) { mutableStateOf<String?>(null) }
    var tempVideoPath by rememberSaveable(occurrence.id) { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)
    ) { uris ->
        val savedPaths = uris.mapNotNull { FileUtils.copyUriToInternalStorage(context, it) }
        if (savedPaths.isNotEmpty()) {
            resolutionImages = resolutionImages + savedPaths
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoPath != null) {
            ImageUtils.compressImage(tempPhotoPath!!)
            ImageUtils.createThumbnail(context, tempPhotoPath!!)
            resolutionImages = resolutionImages + tempPhotoPath!!
        }
        tempPhotoPath = null
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoPath != null) {
            resolutionVideos = resolutionVideos + tempVideoPath!!
        }
        tempVideoPath = null
    }

    if (showInfoDialog) {
        val dialogTitle = when {
            infoTargetStatus == TicketStatus.EM_RESOLUCAO ->
                stringResource(R.string.dialog_provide_info_title)
            currentProfile == Profile.OPERATOR ->
                stringResource(R.string.dialog_request_info_title)
            else ->
                stringResource(R.string.dialog_request_clarification_title)
        }
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(dialogTitle) },
            text = {
                OutlinedTextField(
                    value = infoMessageText,
                    onValueChange = { infoMessageText = it },
                    label = { Text(stringResource(R.string.dialog_info_message_label)) },
                    placeholder = { Text(stringResource(R.string.dialog_info_message_placeholder)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = infoMessageText.isNotBlank(),
                    onClick = {
                        onSendMessage(infoMessageText.trim())
                        onChangeStatus(infoTargetStatus)
                        infoMessageText = ""
                        showInfoDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        infoMessageText = ""
                        showInfoDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showReopenDialog) {
        AlertDialog(
            onDismissRequest = { showReopenDialog = false },
            title = { Text(stringResource(R.string.detail_reopen_title)) },
            text = {
                OutlinedTextField(
                    value = justification,
                    onValueChange = { justification = it },
                    label = { Text(stringResource(R.string.detail_justification_label)) },
                    placeholder = { Text(stringResource(R.string.detail_justification_placeholder)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = justification.isNotBlank(),
                    onClick = {
                        onReopen(justification)
                        justification = ""
                        showReopenDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        justification = ""
                        showReopenDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showResolveDialog) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = { Text(stringResource(R.string.detail_resolve_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.detail_resolve_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 4.dp).size(16.dp))
                            Text(stringResource(R.string.btn_gallery))
                        }
                        OutlinedButton(
                            onClick = {
                                val (file, uri) = FileUtils.createTempMediaFile(context, isVideo = false)
                                tempPhotoPath = file.absolutePath
                                takePictureLauncher.launch(uri)
                            }
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = 4.dp).size(16.dp))
                            Text(stringResource(R.string.btn_photo))
                        }
                        OutlinedButton(
                            onClick = {
                                val (file, uri) = FileUtils.createTempMediaFile(context, isVideo = true)
                                tempVideoPath = file.absolutePath
                                captureVideoLauncher.launch(uri)
                            }
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.padding(end = 4.dp).size(16.dp))
                            Text(stringResource(R.string.btn_video))
                        }
                    }
                    Text(
                        text = stringResource(R.string.detail_attached_count, resolutionImages.size, resolutionVideos.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (resolutionImages.isEmpty() && resolutionVideos.isEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = resolutionImages.isNotEmpty() || resolutionVideos.isNotEmpty(),
                    onClick = {
                        onResolve(resolutionImages, resolutionVideos)
                        resolutionImages = emptyList()
                        resolutionVideos = emptyList()
                        showResolveDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        resolutionImages = emptyList()
                        resolutionVideos = emptyList()
                        showResolveDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bloco 1: Cabeçalho e Estado
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = occurrence.situation.displayLabel(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.detail_category_format, categoryName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentProfile != Profile.ANONYMOUS) {
                        StatusBadge(status = occurrence.status)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.detail_created_at, createdAtFormatted),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (occurrence.groupOccurrence) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Group Issue • ${occurrence.followersIds.size} follower(s)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Bloco 2: Descrição e Metadados
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.detail_details)
                )

                Text(
                    text = occurrence.description,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (currentProfile != Profile.ANONYMOUS) {
                    HorizontalDivider()
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = stringResource(R.string.detail_author),
                        value = author?.name ?: occurrence.authorId
                    )
                    if (currentProfile == Profile.OPERATOR && author != null) {
                        InfoRow(
                            icon = Icons.Default.Email,
                            label = stringResource(R.string.field_email),
                            value = author.email
                        )
                        InfoRow(
                            icon = Icons.Default.Phone,
                            label = stringResource(R.string.field_phone),
                            value = author.phone
                        )
                    }
                    InfoRow(
                        icon = if (occurrence.range == Visibility.PUBLIC) {
                            Icons.Default.Public
                        } else {
                            Icons.Default.Lock
                        },
                        label = stringResource(R.string.label_visibility),
                        value = occurrence.range.displayLabel()
                    )
                }
            }
        }

        // Bloco 2B: Evidências do Problema e da Resolução
        MediaGallerySection(
            title = stringResource(R.string.detail_problem_evidence),
            icon = Icons.Default.PhotoLibrary,
            imagePaths = occurrence.imagePaths,
            videoPaths = occurrence.videoPaths
        )

        MediaGallerySection(
            title = stringResource(R.string.detail_resolution_proof),
            icon = Icons.Default.CheckCircle,
            imagePaths = occurrence.resolutionImagePaths,
            videoPaths = occurrence.resolutionVideoPaths
        )

        // mapa
        occurrence.location?.let { loc ->
            val isAnonymous = currentProfile == Profile.ANONYMOUS
            val occLatLng = LatLng(loc.latitude, loc.longitude)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    occLatLng,
                    if (isAnonymous) 12f else 15f // zoom afastado para Anónimo!
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(R.string.detail_location)
                    )

                    // Morada exata oculta para Anónimo
                    if (!isAnonymous && !occurrence.address.isNullOrBlank()) {
                        Text(
                            text = occurrence.address,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (currentProfile == Profile.OPERATOR) {
                        Text(
                            text = "Coordinates: %.5f, %.5f".format(loc.latitude, loc.longitude),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        cameraPositionState = cameraPositionState
                    ) {
                        // Marcador exato apenas para utilizadores autenticados
                        if (!isAnonymous) {
                            val markerState = remember(occLatLng) {
                                MarkerState(position = occLatLng)
                            }
                            Marker(
                                state = markerState,
                                title = occurrence.situation.displayLabel(),
                                snippet = occurrence.address
                            )
                        }
                    }
                }
            }
        }

        // Bloco 3: Ações (visível apenas quando há ações disponíveis para o perfil/estado)
        val hasOperatorActions = currentProfile == Profile.OPERATOR &&
            occurrence.status != TicketStatus.RESOLVIDO &&
            occurrence.status != TicketStatus.ENCERRADO
        val hasCitizenActions = currentProfile == Profile.CITIZEN &&
            (occurrence.status == TicketStatus.ABERTO ||
                occurrence.status == TicketStatus.EM_RESOLUCAO ||
                occurrence.status == TicketStatus.PEDIDO_DE_INFORMACOES ||
                (occurrence.status == TicketStatus.RESOLVIDO && canReopen))

        if (hasOperatorActions || hasCitizenActions) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.detail_actions)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentProfile == Profile.OPERATOR) {
                            when (occurrence.status) {
                                TicketStatus.ABERTO -> {
                                    ActionOutlinedButton(
                                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                                        text = stringResource(R.string.btn_request_info),
                                        onClick = {
                                            infoTargetStatus = TicketStatus.PEDIDO_DE_INFORMACOES
                                            showInfoDialog = true
                                        }
                                    )
                                    ActionButton(
                                        icon = Icons.Default.PlayArrow,
                                        text = stringResource(R.string.btn_start_resolution),
                                        onClick = { onChangeStatus(TicketStatus.EM_RESOLUCAO) }
                                    )
                                }
                                TicketStatus.PEDIDO_DE_INFORMACOES -> {
                                    ActionButton(
                                        icon = Icons.Default.PlayArrow,
                                        text = stringResource(R.string.btn_start_resolution),
                                        onClick = { onChangeStatus(TicketStatus.EM_RESOLUCAO) }
                                    )
                                }
                                TicketStatus.EM_RESOLUCAO -> {
                                    ActionOutlinedButton(
                                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                                        text = stringResource(R.string.btn_request_info),
                                        onClick = {
                                            infoTargetStatus = TicketStatus.PEDIDO_DE_INFORMACOES
                                            showInfoDialog = true
                                        }
                                    )
                                    ActionButton(
                                        icon = Icons.Default.CheckCircle,
                                        text = stringResource(R.string.btn_mark_resolved),
                                        onClick = { showResolveDialog = true }
                                    )
                                }
                                TicketStatus.RESOLVIDO, TicketStatus.ENCERRADO -> {}
                            }
                        }

                        if (currentProfile == Profile.CITIZEN) {
                            if (occurrence.status == TicketStatus.ABERTO ||
                                occurrence.status == TicketStatus.EM_RESOLUCAO
                            ) {
                                ActionOutlinedButton(
                                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                                    text = stringResource(R.string.btn_request_clarification),
                                    onClick = {
                                        infoTargetStatus = TicketStatus.PEDIDO_DE_INFORMACOES
                                        showInfoDialog = true
                                    }
                                )
                            }
                            if (occurrence.status == TicketStatus.PEDIDO_DE_INFORMACOES) {
                                ActionButton(
                                    icon = Icons.Default.CheckCircle,
                                    text = stringResource(R.string.btn_info_provided),
                                    onClick = {
                                        infoTargetStatus = TicketStatus.EM_RESOLUCAO
                                        showInfoDialog = true
                                    }
                                )
                            }
                            if (occurrence.status == TicketStatus.RESOLVIDO && canReopen) {
                                ActionButton(
                                    icon = Icons.Default.Refresh,
                                    text = stringResource(R.string.btn_reopen_ticket),
                                    onClick = { showReopenDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bloco 4: Histórico de Estados (apenas para o Operador)
        if (currentProfile == Profile.OPERATOR) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.detail_status_history),
                        isProminent = true
                    )

                    if (occurrence.statusHistory.isEmpty()) {
                        Text(
                            text = stringResource(R.string.detail_no_status_changes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        occurrence.statusHistory.forEachIndexed { index, change ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBadge(status = change.status)
                                    Text(
                                        text = dateFormat.format(Date(change.timestamp)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!change.whyReopen.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append(stringResource(R.string.detail_justification_prefix))
                                                }
                                                append(change.whyReopen)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            )
                                        )
                                    }
                                }
                            }

                            if (index < occurrence.statusHistory.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        OccurrenceMessagesSection(
            messages = messages,
            currentSenderRole = when (currentProfile) {
                Profile.ANONYMOUS -> null
                Profile.CITIZEN -> UserRole.MUNICIPE
                Profile.OPERATOR -> UserRole.OPERADOR
            },
            enabled = occurrence.status != TicketStatus.ENCERRADO,
            onSendMessage = onSendMessage
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MediaGallerySection(
    title: String,
    icon: ImageVector,
    imagePaths: List<String>,
    videoPaths: List<String>
) {
    if (imagePaths.isEmpty() && videoPaths.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(icon = icon, title = title)

            if (imagePaths.isNotEmpty()) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(imagePaths) { path ->
                        AsyncImage(
                            model = path,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            if (videoPaths.isNotEmpty()) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(videoPaths) { path ->
                        VideoThumbnailItem(
                            path = path,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    isProminent: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(if (isProminent) 26.dp else 22.dp)
        )
        Text(
            text = title,
            style = if (isProminent) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Button(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(18.dp)
        )
        Text(text)
    }
}

@Composable
private fun ActionOutlinedButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(18.dp)
        )
        Text(text)
    }
}
