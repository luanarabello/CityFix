package pt.isec.amov.cityfix.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.ui.components.MapLocationPicker
import pt.isec.amov.cityfix.ui.components.UserAvatar
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.LocationUtils
import pt.isec.amov.cityfix.utils.ImageUtils
import pt.isec.amov.cityfix.viewmodel.ProfileUiState

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNifChange: (String) -> Unit,
    onPictureChange: (String?) -> Unit,
    onImpactDetectionChange: (Boolean) -> Unit,
    onLocationSelected: (Double, Double) -> Unit = { _, _ -> },
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UserProfileForm(
        title = stringResource(R.string.profile_title),
        submitLabel = stringResource(R.string.btn_save_changes),
        state = state,
        onNameChange = onNameChange,
        onEmailChange = onEmailChange,
        onPhoneChange = onPhoneChange,
        onNifChange = onNifChange,
        onPictureChange = onPictureChange,
        onImpactDetectionChange = onImpactDetectionChange,
        onLocationSelected = onLocationSelected,
        onSubmitClick = onSaveClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UserProfileForm(
    title: String,
    submitLabel: String,
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNifChange: (String) -> Unit,
    onPictureChange: (String?) -> Unit,
    onImpactDetectionChange: (Boolean) -> Unit,
    onLocationSelected: (Double, Double) -> Unit = { _, _ -> },
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var temporaryPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            FileUtils.copyUriToInternalStorage(context, selectedUri)?.let(onPictureChange)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val photoPath = temporaryPhotoPath
        if (success && photoPath != null) {
            ImageUtils.compressImage(photoPath)
            ImageUtils.createThumbnail(context, photoPath)
            onPictureChange(photoPath)
        } else {
            FileUtils.deleteFileFromPath(photoPath)
        }
        temporaryPhotoPath = null
    }

    Scaffold(modifier = modifier.imePadding()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val wideLayout = maxWidth >= 720.dp
                    val fieldWidth = if (wideLayout) (maxWidth - 12.dp) / 2 else maxWidth
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = if (wideLayout) 2 else 1
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            UserAvatar(
                                name = state.name,
                                picturePath = state.picturePath,
                                size = 88.dp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    enabled = !state.isSaving
                                ) {
                                    Text(stringResource(R.string.btn_choose_photo))
                                }
                                OutlinedButton(
                                    onClick = {
                                        val (file, uri) = FileUtils.createTempMediaFile(context)
                                        temporaryPhotoPath = file.absolutePath
                                        takePictureLauncher.launch(uri)
                                    },
                                    enabled = !state.isSaving
                                ) {
                                    Text(stringResource(R.string.btn_take_photo))
                                }
                                if (!state.picturePath.isNullOrBlank()) {
                                    TextButton(
                                        onClick = { onPictureChange(null) },
                                        enabled = !state.isSaving
                                    ) {
                                        Text(stringResource(R.string.btn_remove_photo))
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.name,
                            onValueChange = onNameChange,
                            label = { Text(stringResource(R.string.field_name)) },
                            isError = state.nameError != null,
                            supportingText = {
                                state.nameError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            enabled = state.isRegistration && !state.isSaving,
                            singleLine = true,
                            modifier = Modifier.width(fieldWidth)
                        )
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = onEmailChange,
                            label = { Text(stringResource(R.string.field_email)) },
                            isError = state.emailError != null,
                            supportingText = {
                                state.emailError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            enabled = !state.isSaving,
                            singleLine = true,
                            modifier = Modifier.width(fieldWidth)
                        )
                        OutlinedTextField(
                            value = state.phone,
                            onValueChange = onPhoneChange,
                            label = { Text(stringResource(R.string.field_phone)) },
                            isError = state.phoneError != null,
                            supportingText = {
                                state.phoneError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            enabled = !state.isSaving,
                            singleLine = true,
                            modifier = Modifier.width(fieldWidth)
                        )
                        OutlinedTextField(
                            value = state.nif,
                            onValueChange = onNifChange,
                            label = { Text(stringResource(R.string.field_nif)) },
                            isError = state.nifError != null,
                            supportingText = {
                                state.nifError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = state.isRegistration && !state.isSaving,
                            singleLine = true,
                            modifier = Modifier.width(fieldWidth)
                        )
                        val canEditLocation = state.profileType == UserRole.OPERADOR
                        if (state.location != null || canEditLocation) {
                            val loc = state.location
                            var resolvedAddress by remember(loc) { mutableStateOf("") }
                            LaunchedEffect(loc) {
                                resolvedAddress = if (loc != null) {
                                    LocationUtils.reverseGeocode(context, loc.latitude, loc.longitude)
                                } else {
                                    ""
                                }
                            }
                            val locationDisplay = when {
                                loc == null -> stringResource(R.string.profile_no_location)
                                resolvedAddress.isNotBlank() ->
                                    "$resolvedAddress (${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)})"
                                else -> "${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}"
                            }
                            OutlinedTextField(
                                value = locationDisplay,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.field_location)) },
                                enabled = false,
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (canEditLocation) {
                                val coroutineScope = rememberCoroutineScope()
                                var addressQuery by rememberSaveable { mutableStateOf("") }
                                LaunchedEffect(resolvedAddress) {
                                    if (resolvedAddress.isNotBlank()) {
                                        addressQuery = resolvedAddress
                                    }
                                }
                                val searchAddress: () -> Unit = {
                                    coroutineScope.launch {
                                        LocationUtils.geocodeAddress(context, addressQuery)?.let { found ->
                                            onLocationSelected(found.latitude, found.longitude)
                                        }
                                    }
                                }
                                val locationPermissionLauncher = rememberLauncherForActivityResult(
                                    contract = ActivityResultContracts.RequestMultiplePermissions()
                                ) { permissions ->
                                    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                                    if (granted) {
                                        coroutineScope.launch {
                                            LocationUtils.getCurrentLocation(context)?.let { gps ->
                                                onLocationSelected(gps.latitude, gps.longitude)
                                            }
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    enabled = !state.isSaving
                                ) {
                                    Text(stringResource(R.string.btn_use_current_location))
                                }
                                OutlinedTextField(
                                    value = addressQuery,
                                    onValueChange = { addressQuery = it },
                                    label = { Text(stringResource(R.string.field_address_location)) },
                                    singleLine = true,
                                    enabled = !state.isSaving,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { searchAddress() }),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = searchAddress,
                                            enabled = addressQuery.isNotBlank() && !state.isSaving
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = stringResource(R.string.cd_search_address)
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MapLocationPicker(
                                    initialLat = loc?.latitude,
                                    initialLng = loc?.longitude,
                                    onLocationSelected = { lat, lng, addr ->
                                        if (addr.isNotBlank()) addressQuery = addr
                                        onLocationSelected(lat, lng)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.profile_impact_detection), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(R.string.profile_impact_detection_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.impactDetection,
                                onCheckedChange = onImpactDetectionChange,
                                enabled = !state.isSaving
                            )
                        }
                    }
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (state.isSaved) {
                Text(stringResource(R.string.profile_changes_saved), color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = onSubmitClick,
                enabled = state.isValid && !state.isLoading && !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator()
                } else {
                    Text(submitLabel)
                }
            }
        }
    }
}
