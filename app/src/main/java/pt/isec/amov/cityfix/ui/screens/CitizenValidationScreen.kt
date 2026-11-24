package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.utils.LocationUtils
import pt.isec.amov.cityfix.viewmodel.CitizenValidationUiState

@Composable
fun CitizenValidationScreen(
    state: CitizenValidationUiState,
    onApproveCitizen: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.validation_pending_citizens),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.isLoading
                ) {
                    Text(stringResource(R.string.btn_refresh), maxLines = 1, softWrap = false)
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                }

                state.pendingCitizens.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.validation_empty),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.pendingCitizens,
                            key = User::id
                        ) { citizen ->
                            PendingCitizenCard(
                                citizen = citizen,
                                isApproving = citizen.id in state.approvingUserIds,
                                onApprove = { onApproveCitizen(citizen.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingCitizenCard(
    citizen: User,
    isApproving: Boolean,
    onApprove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = citizen.name.ifBlank { stringResource(R.string.validation_unnamed) },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.validation_nif_format,
                    citizen.nif.ifBlank { stringResource(R.string.validation_not_provided) }
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            val context = LocalContext.current
            val loc = citizen.location
            var resolvedAddress by remember(loc) { mutableStateOf("") }
            LaunchedEffect(loc) {
                resolvedAddress = if (loc != null) {
                    LocationUtils.reverseGeocode(context, loc.latitude, loc.longitude)
                } else {
                    ""
                }
            }
            val locationValue = when {
                loc == null -> stringResource(R.string.validation_not_provided)
                resolvedAddress.isNotBlank() ->
                    "$resolvedAddress (${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)})"
                else -> "${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}"
            }
            Text(
                text = stringResource(R.string.validation_location_format, locationValue),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onApprove,
                enabled = !isApproving
            ) {
                if (isApproving) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.btn_approve))
                }
            }
        }
    }
}
