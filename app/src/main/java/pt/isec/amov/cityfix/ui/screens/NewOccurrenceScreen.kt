package pt.isec.amov.cityfix.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.ui.components.VideoThumbnailItem
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.Visibility
import pt.isec.amov.cityfix.ui.components.MapLocationPicker
import pt.isec.amov.cityfix.ui.components.displayLabel
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.ImageUtils
import pt.isec.amov.cityfix.utils.LocationUtils
import pt.isec.amov.cityfix.viewmodel.NewOccurrenceUiState
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewOccurrenceScreen(
    state: NewOccurrenceUiState,
    onCategorySelected: (String) -> Unit,
    onSituationSelected: (Situation) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onVisibilitySelected: (Visibility) -> Unit,
    onImagesAdded: (List<String>) -> Unit,
    onImageRemoved: (String) -> Unit,
    onVideoAdded: (String) -> Unit,
    onVideoRemoved: (String) -> Unit,
    onSaveClick: () -> Unit,
    onAddressChange: (String) -> Unit,
    onLocationSelected: (lat: Double, lng: Double, address: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = state.occurrenceId != null
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ){ permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            coroutineScope.launch {
                LocationUtils.getCurrentLocation(context)?.let { loc ->
                    val addr = LocationUtils.reverseGeocode(context, loc.latitude, loc.longitude)
                    onLocationSelected(loc.latitude, loc.longitude, addr)
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)
    ) { uris ->
        val savedPaths = uris.mapNotNull { FileUtils.copyUriToInternalStorage(context, it) }
        if (savedPaths.isNotEmpty()) {
            savedPaths.forEach {
                ImageUtils.compressImage(it)
                ImageUtils.createThumbnail(context, it)
            }
            onImagesAdded(savedPaths)
        }
    }

    var tempPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoPath != null) {
            ImageUtils.compressImage(tempPhotoPath!!)
            ImageUtils.createThumbnail(context, tempPhotoPath!!)
            onImagesAdded(listOf(tempPhotoPath!!))
        }
        tempPhotoPath = null
    }

    var tempVideoPath by rememberSaveable { mutableStateOf<String?>(null) }
    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoPath != null) {
            onVideoAdded(tempVideoPath!!)
        }
        tempVideoPath = null
    }

    Scaffold(
        modifier = modifier.imePadding(), // ajusta o ecrã acima do teclado
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (state.isValid && !state.isSaving) {
                        onSaveClick()
                    }
                },
                containerColor = if (state.isValid) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (state.isValid) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = if (state.isValid) 6.dp else 0.dp
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.cd_save_occurrence)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabeçalho visual da secção
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (isEditing) stringResource(R.string.occurrence_edit_title) else stringResource(R.string.occurrence_new_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.occurrence_form_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val sectionWidth = if (maxWidth >= 720.dp) (maxWidth - 16.dp) / 2 else maxWidth
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = if (maxWidth >= 720.dp) 2 else 1
                ) {
            // Secção 1: Classificação (Categoria, Situação e Alcance)
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.width(sectionWidth)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_classification),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    DropdownSelection(
                        label = stringResource(R.string.label_category),
                        selectedText = state.availableCategories.find { it.id == state.categoryId }?.title ?: "",
                        options = state.availableCategories,
                        optionLabel = { it.title },
                        onOptionSelected = { onCategorySelected(it.id) },
                        isError = state.categoryError != null,
                        errorMessage = state.categoryError
                    )

                    DropdownSelection(
                        label = stringResource(R.string.label_situation),
                        selectedText = state.situation.displayLabel(),
                        options = Situation.entries,
                        optionLabel = { it.displayLabel(context) },
                        onOptionSelected = onSituationSelected
                    )

                    DropdownSelection(
                        label = stringResource(R.string.label_visibility),
                        selectedText = state.range.displayLabel(),
                        options = Visibility.entries,
                        optionLabel = { it.displayLabel(context) },
                        onOptionSelected = onVisibilitySelected
                    )
                }
            }

            // Secção 2: Descrição
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.width(sectionWidth)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_details_required),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        label = { Text(stringResource(R.string.label_description_required)) },
                        placeholder = { Text(stringResource(R.string.placeholder_description)) },
                        isError = state.descriptionError != null,
                        supportingText = {
                            Text(state.descriptionError ?: stringResource(R.string.helper_description_required))
                        },
                        minLines = 4,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Secção 3: Evidências (Obrigatório: pelo menos 1 foto ou vídeo)
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.width(sectionWidth)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_evidence_required),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
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
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                            )
                            Text(stringResource(R.string.btn_gallery))
                        }

                        OutlinedButton(
                            onClick = {
                                val (file, uri) = FileUtils.createTempMediaFile(context, isVideo = false)
                                tempPhotoPath = file.absolutePath
                                takePictureLauncher.launch(uri)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                            )
                            Text(stringResource(R.string.btn_photo))
                        }

                        OutlinedButton(
                            onClick = {
                                val (file, uri) = FileUtils.createTempMediaFile(context, isVideo = true)
                                tempVideoPath = file.absolutePath
                                captureVideoLauncher.launch(uri)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                            )
                            Text(stringResource(R.string.btn_video))
                        }
                    }

                    if (state.imagePaths.isEmpty() && state.videoPaths.isEmpty()) {
                        Text(
                            text = stringResource(R.string.helper_media_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (state.imagePaths.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.imagePaths) { path ->
                                Box(modifier = Modifier.size(96.dp)) {
                                    AsyncImage(
                                        model = path,
                                        contentDescription = "Attached photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    IconButton(
                                        onClick = { onImageRemoved(path) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove photo",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.videoPaths.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.videoPaths) { path ->
                                VideoThumbnailItem(
                                    path = path,
                                    onRemove = { onVideoRemoved(path) },
                                    modifier = Modifier.size(84.dp)
                                )
                            }
                        }
                    }
                }
            }
                }
            }

            Surface( // faz tipo uma modal
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_location_required),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.btn_use_current_location))
                    }

                    val searchAddress: () -> Unit = {
                        coroutineScope.launch {
                            LocationUtils.geocodeAddress(context, state.address)?.let { loc ->
                                onLocationSelected(loc.latitude, loc.longitude, state.address)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.address,
                        onValueChange = onAddressChange,
                        label = { Text(stringResource(R.string.field_address_location)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { searchAddress() }),
                        trailingIcon = {
                            IconButton(
                                onClick = searchAddress,
                                enabled = state.address.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.cd_search_address)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.latitude == null || state.longitude == null) {
                        Text(
                            text = stringResource(R.string.helper_location_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    MapLocationPicker(
                        initialLat = state.latitude,
                        initialLng = state.longitude,
                        onLocationSelected = onLocationSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSelection(
    label: String,
    selectedText: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = isError,
            supportingText = errorMessage?.let { msg -> { Text(msg) } },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
