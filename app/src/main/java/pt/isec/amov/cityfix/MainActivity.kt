package pt.isec.amov.cityfix

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.messaging.FirebaseMessaging
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.ui.components.displayLabel
import pt.isec.amov.cityfix.utils.NotificationHelper
import java.util.concurrent.TimeUnit
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pt.isec.amov.cityfix.services.ProximityService
import pt.isec.amov.cityfix.services.sensors.ImpactDetector
import pt.isec.amov.cityfix.ui.navigation.CitizenValidationRoute
import pt.isec.amov.cityfix.ui.navigation.CityFixNavHost
import pt.isec.amov.cityfix.ui.navigation.DashboardRoute
import pt.isec.amov.cityfix.ui.navigation.EditOccurrenceRoute
import pt.isec.amov.cityfix.ui.navigation.OccurrenceListRoute
import pt.isec.amov.cityfix.ui.navigation.LoginRoute
import pt.isec.amov.cityfix.ui.navigation.OperatorCategoriesRoute
import pt.isec.amov.cityfix.ui.navigation.Profile
import pt.isec.amov.cityfix.ui.navigation.ProfileRoute
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.OccurrencesViewModel
import pt.isec.amov.cityfix.viewmodel.OccurrencesViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CityFixTheme {
                val app = application as CityFixApp
                val viewModel: OccurrencesViewModel = viewModel(
                    factory = OccurrencesViewModelFactory(app.occurrenceRepository)
                )
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val isListScreen = navBackStackEntry?.destination?.hasRoute<OccurrenceListRoute>() == true
                val isLoginScreen = navBackStackEntry == null || navBackStackEntry?.destination?.hasRoute<LoginRoute>() == true
                var currentProfile by rememberSaveable { mutableStateOf(Profile.ANONYMOUS) }
                var proximityServiceStarted by remember { mutableStateOf(false) }
                var impactDetectionEnabled by rememberSaveable { mutableStateOf(false) }
                var showImpactSuggestion by rememberSaveable { mutableStateOf(false) }
                val lifecycleOwner = LocalLifecycleOwner.current
                val impactDetector = remember {
                    ImpactDetector(applicationContext) { showImpactSuggestion = true }
                }
                val isCreatingOccurrence =
                    navBackStackEntry?.destination?.hasRoute<EditOccurrenceRoute>() == true
                val shouldMonitorImpacts = currentProfile == Profile.CITIZEN &&
                    impactDetectionEnabled &&
                    !showImpactSuggestion &&
                    !isCreatingOccurrence

                LaunchedEffect(navBackStackEntry?.destination, currentProfile) {
                    val uid = app.authRepository.currentUser?.id
                    impactDetectionEnabled = try {
                        if (uid != null) {
                            app.userRepository.loadUsers()
                                .findById(uid)
                                ?.impactDetection ?: false
                        } else {
                            false
                        }
                    } catch (_: Exception) {
                        false
                    }
                }

                DisposableEffect(lifecycleOwner, impactDetector, shouldMonitorImpacts) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> if (shouldMonitorImpacts) impactDetector.start()
                            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> impactDetector.stop()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                        shouldMonitorImpacts
                    ) {
                        impactDetector.start()
                    }
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        impactDetector.stop()
                    }
                }

                Scaffold(topBar = {
                    if (!isLoginScreen) {
                        CityFixTopAppBar(
                            isListScreen,
                            currentProfile,
                            onNavigateBack = { navController.navigateUp() },
                            onDashboardClick = { navController.navigate(DashboardRoute) },
                            onProfileClick = {
                                app.authRepository.currentUser?.id?.let { uid ->
                                    navController.navigate(ProfileRoute(uid))
                                }
                            },
                            onAddOccurrenceClick = { navController.navigate(EditOccurrenceRoute(null)) },
                            onManageCategoriesClick = { navController.navigate(OperatorCategoriesRoute) },
                            onValidateCitizensClick = { navController.navigate(CitizenValidationRoute) },
                            onLoginClick = { navController.navigate(LoginRoute) },
                            onLogoutClick = {
                                app.authRepository.signOut()
                                currentProfile = Profile.ANONYMOUS
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }) { innerPadding ->
                    CityFixNavHost(
                        repository = app.occurrenceRepository,
                        userRepository = app.userRepository,
                        categoryRepository = app.categoryRepository,
                        authRepository = app.authRepository,
                        navController = navController,
                        viewModel = viewModel,
                        currentProfile = currentProfile,
                        onAuthenticated = { authUser ->
                            currentProfile = if (authUser.email?.endsWith("@isec.pt", ignoreCase = true) == true) {
                                Profile.OPERATOR
                            } else {
                                Profile.CITIZEN
                            }
                            navController.navigate(OccurrenceListRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onContinueAsAnonymous = {
                            currentProfile = Profile.ANONYMOUS
                            navController.navigate(OccurrenceListRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onImpactDetectionChanged = { impactDetectionEnabled = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                val locationPermissionRequest = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (fineLocation || coarseLocation) {
                        val intent = Intent(this, ProximityService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                        proximityServiceStarted = true
                    }
                }

                LaunchedEffect(isLoginScreen) {
                    if (isLoginScreen) {
                        if (proximityServiceStarted) {
                            stopService(Intent(this@MainActivity, ProximityService::class.java))
                            proximityServiceStarted = false
                        }
                    } else {
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        locationPermissionRequest.launch(permissionsToRequest.toTypedArray())
                        FirebaseMessaging.getInstance().subscribeToTopic("cityfix")
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.i("AMOV: FCM", "Subscribed to topic cityfix")
                                }
                            }
                    }
                }

                val messagesByOccurrence by viewModel.messagesByOccurrence.collectAsStateWithLifecycle()
                val occurrences by viewModel.occurrencesList.collectAsStateWithLifecycle()
                val appLaunchTime = remember { System.currentTimeMillis() }
                val notifiedMessageKeys = remember { mutableSetOf<String>() }
                val lastKnownStatuses = remember { mutableMapOf<String, TicketStatus>() }
                val notified24hKeys = remember { mutableSetOf<String>() }

                // 1. Notificações de mensagens / pedidos de informação e respostas
                LaunchedEffect(messagesByOccurrence, occurrences, currentProfile, isLoginScreen) {
                    if (isLoginScreen || currentProfile == Profile.ANONYMOUS) return@LaunchedEffect

                    val myRole = when (currentProfile) {
                        Profile.CITIZEN -> UserRole.MUNICIPE
                        Profile.OPERATOR -> UserRole.OPERADOR
                        Profile.ANONYMOUS -> return@LaunchedEffect
                    }
                    val currentUid = app.authRepository.currentUser?.id

                    messagesByOccurrence.values.flatten().forEach { msg ->
                        if (msg.timestamp < appLaunchTime || msg.senderRole == myRole) return@forEach
                        val key = "$currentProfile:${msg.id}"
                        if (!notifiedMessageKeys.add(key)) return@forEach

                        val occ = occurrences.find { it.id == msg.occurrenceId }
                        val isRelevant = if (currentProfile == Profile.CITIZEN) {
                            currentUid != null && occ != null &&
                                (occ.authorId == currentUid || currentUid in occ.followersIds)
                        } else {
                            true
                        }
                        if (isRelevant) {
                            NotificationHelper.showNotification(
                                context = this@MainActivity,
                                channelId = "info_requests",
                                channelName = getString(R.string.notif_info_channel_name),
                                notificationId = key.hashCode(),
                                title = getString(R.string.notif_info_title),
                                body = msg.text
                            )
                        }
                    }
                }

                // 2. Notificações de mudança de estado e alerta de 24h antes do fecho definitivo
                LaunchedEffect(occurrences, currentProfile, isLoginScreen) {
                    if (isLoginScreen || currentProfile == Profile.ANONYMOUS || occurrences.isEmpty()) {
                        return@LaunchedEffect
                    }

                    val currentUid = app.authRepository.currentUser?.id
                    val now = System.currentTimeMillis()
                    val fiveDaysMillis = TimeUnit.DAYS.toMillis(5)
                    val oneDayMillis = TimeUnit.HOURS.toMillis(24)

                    occurrences.forEach { occ ->
                        val statusKey = "$currentProfile:${occ.id}"
                        val previousStatus = lastKnownStatuses.put(statusKey, occ.status)
                        val isRelevant = when (currentProfile) {
                            Profile.OPERATOR -> true
                            Profile.CITIZEN -> currentUid != null &&
                                (occ.authorId == currentUid || currentUid in occ.followersIds)
                            Profile.ANONYMOUS -> false
                        }

                        if (isRelevant && previousStatus != null && previousStatus != occ.status) {
                            NotificationHelper.showNotification(
                                context = this@MainActivity,
                                channelId = "status_updates",
                                channelName = getString(R.string.notif_status_channel_name),
                                notificationId = ("status:${occ.id}:${occ.status}").hashCode(),
                                title = getString(R.string.notif_status_title),
                                body = getString(
                                    R.string.notif_status_body,
                                    occ.situation.displayLabel(this@MainActivity),
                                    occ.status.displayLabel(this@MainActivity)
                                )
                            )
                        }

                        if (currentProfile == Profile.CITIZEN &&
                            isRelevant &&
                            occ.status == TicketStatus.RESOLVIDO
                        ) {
                            val resolvedAt = occ.statusHistory
                                .lastOrNull { it.status == TicketStatus.RESOLVIDO }
                                ?.timestamp ?: occ.createdAt
                            val remaining = (resolvedAt + fiveDaysMillis) - now
                            if (remaining in 1..oneDayMillis) {
                                val key24h = "24h:${occ.id}:$currentUid"
                                if (notified24hKeys.add(key24h)) {
                                    NotificationHelper.showNotification(
                                        context = this@MainActivity,
                                        channelId = "status_updates",
                                        channelName = getString(R.string.notif_status_channel_name),
                                        notificationId = key24h.hashCode(),
                                        title = getString(R.string.notif_closing_24h_title),
                                        body = getString(
                                            R.string.notif_closing_24h_body,
                                            occ.situation.displayLabel(this@MainActivity)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    viewModel.checkExpiredResolved()
                }

                if (showImpactSuggestion) {
                    AlertDialog(
                        onDismissRequest = { showImpactSuggestion = false },
                        title = { Text(stringResource(R.string.impact_dialog_title)) },
                        text = {
                            Text(stringResource(R.string.impact_dialog_message))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showImpactSuggestion = false
                                    navController.navigate(EditOccurrenceRoute(null))
                                }
                            ) { Text(stringResource(R.string.impact_dialog_open)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImpactSuggestion = false }) {
                                Text(stringResource(R.string.impact_dialog_dismiss))
                            }
                        }
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class) //só recebe dados e callbacks, é stateless
@Composable
fun CityFixTopAppBar(
    isListScreen: Boolean,
    currentProfile: Profile,
    onNavigateBack: () -> Unit,
    onDashboardClick: () -> Unit,
    onLoginClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAddOccurrenceClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onValidateCitizensClick: () -> Unit,
    onLogoutClick: () -> Unit
){
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        navigationIcon = {
            if (!isListScreen) {
                IconButton(
                    onClick = onNavigateBack
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back)) }
            }
        },
        actions = { if(isListScreen){
                IconButton(onClick = onDashboardClick) {
                    Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.cd_dashboard))
                }
                if(currentProfile == Profile.ANONYMOUS){
                    IconButton(onClick = onLoginClick) {
                        Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.cd_sign_in_or_register))
                    }
                }
                if (currentProfile == Profile.OPERATOR) {
                    IconButton(onClick = onManageCategoriesClick) {
                        Icon(Icons.Default.Category, contentDescription = stringResource(R.string.cd_manage_categories))
                    }
                    IconButton(onClick = onValidateCitizensClick) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = stringResource(R.string.cd_validate_citizens))
                    }
                }
                if(currentProfile == Profile.CITIZEN){
                    IconButton(onClick = onAddOccurrenceClick) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_occurrence))
                    }
                }
                if (currentProfile == Profile.CITIZEN || currentProfile == Profile.OPERATOR) {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = stringResource(R.string.cd_profile))
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.cd_sign_out))
                    }
                }
            }
        }

    )
}
