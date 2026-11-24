package pt.isec.amov.cityfix.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthUser
import pt.isec.amov.cityfix.ui.components.MapLocationPicker
import pt.isec.amov.cityfix.utils.LocationUtils
import pt.isec.amov.cityfix.viewmodel.AuthMode
import pt.isec.amov.cityfix.viewmodel.AuthUiState

@Composable
fun LoginScreen(
    state: AuthUiState,
    onNameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit = {},
    onNifChange: (String) -> Unit = {},
    onAddressChange: (String) -> Unit = {},
    onLocationSelected: (Double, Double, String) -> Unit = { _, _, _ -> },
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onModeChange: (AuthMode) -> Unit,
    onSubmit: () -> Unit,
    onAuthenticated: (AuthUser) -> Unit,
    onContinueAsAnonymous: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
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

    LaunchedEffect(state.authenticatedUser) {
        state.authenticatedUser?.let(onAuthenticated)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when (state.mode) {
                    AuthMode.SIGN_IN -> stringResource(R.string.login_title_sign_in)
                    AuthMode.REGISTER -> stringResource(R.string.login_title_register)
                    AuthMode.PASSWORD_RESET -> stringResource(R.string.login_title_reset)
                },
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = when (state.mode) {
                    AuthMode.SIGN_IN -> stringResource(R.string.login_subtitle_sign_in)
                    AuthMode.REGISTER -> stringResource(R.string.login_subtitle_register)
                    AuthMode.PASSWORD_RESET -> stringResource(R.string.login_subtitle_reset)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.mode == AuthMode.REGISTER) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.field_full_name_required)) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = state.nameError != null,
                    supportingText = { state.nameError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.field_email)) },
                singleLine = true,
                enabled = !state.isLoading,
                isError = state.emailError != null,
                supportingText = { state.emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.mode == AuthMode.REGISTER) {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = onPhoneChange,
                    label = { Text(stringResource(R.string.field_phone_required)) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = state.phoneError != null,
                    supportingText = { state.phoneError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.nif,
                    onValueChange = onNifChange,
                    label = { Text(stringResource(R.string.field_nif_required)) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = state.nifError != null,
                    supportingText = { state.nifError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.field_location_required),
                    style = MaterialTheme.typography.titleSmall,
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
                    },
                    enabled = !state.isLoading
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
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchAddress() }),
                    trailingIcon = {
                        IconButton(
                            onClick = searchAddress,
                            enabled = state.address.isNotBlank() && !state.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search_address)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                state.locationError?.let { error ->
                    Text(
                        text = error,
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
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            if (state.mode != AuthMode.PASSWORD_RESET) {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.field_password)) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = state.passwordError != null,
                    supportingText = { state.passwordError?.let { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.mode == AuthMode.REGISTER) {
                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text(stringResource(R.string.field_confirm_password)) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = state.confirmPasswordError != null,
                    supportingText = { state.confirmPasswordError?.let { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            state.infoMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = onSubmit,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        when (state.mode) {
                            AuthMode.SIGN_IN -> stringResource(R.string.btn_sign_in)
                            AuthMode.REGISTER -> stringResource(R.string.btn_create_account)
                            AuthMode.PASSWORD_RESET -> stringResource(R.string.btn_send_recovery)
                        }
                    )
                }
            }

            when (state.mode) {
                AuthMode.SIGN_IN -> {
                    TextButton(
                        onClick = { onModeChange(AuthMode.PASSWORD_RESET) },
                        enabled = !state.isLoading,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text(stringResource(R.string.btn_forgot_password)) }
                    TextButton(
                        onClick = { onModeChange(AuthMode.REGISTER) },
                        enabled = !state.isLoading,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text(stringResource(R.string.btn_go_to_register)) }
                }

                AuthMode.REGISTER -> TextButton(
                    onClick = { onModeChange(AuthMode.SIGN_IN) },
                    enabled = !state.isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text(stringResource(R.string.btn_already_have_account)) }

                AuthMode.PASSWORD_RESET -> TextButton(
                    onClick = { onModeChange(AuthMode.SIGN_IN) },
                    enabled = !state.isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text(stringResource(R.string.btn_back_to_sign_in)) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            OutlinedButton(
                onClick = onContinueAsAnonymous,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_continue_anonymous))
            }
        }
    }
}
