package pt.isec.amov.cityfix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pt.isec.amov.cityfix.data.CategoryRepository
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.AuthUser
import pt.isec.amov.cityfix.data.OccurrenceRepository
import pt.isec.amov.cityfix.data.UserRepository
import pt.isec.amov.cityfix.model.CategoriesList
import pt.isec.amov.cityfix.model.TicketStatus
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.Visibility
import pt.isec.amov.cityfix.utils.DistanceUtils
import pt.isec.amov.cityfix.ui.screens.CitizenValidationScreen
import pt.isec.amov.cityfix.ui.screens.DashboardScreen
import pt.isec.amov.cityfix.ui.screens.LoginScreen
import pt.isec.amov.cityfix.ui.screens.NewOccurrenceScreen
import pt.isec.amov.cityfix.ui.screens.OccurrenceDetailScreen
import pt.isec.amov.cityfix.ui.screens.OperatorCategoriesScreen
import pt.isec.amov.cityfix.ui.screens.OccurrencesListScreen
import pt.isec.amov.cityfix.ui.screens.ProfileScreen
import pt.isec.amov.cityfix.ui.screens.RegisterScreen
import pt.isec.amov.cityfix.viewmodel.NewOccurrenceViewModel
import pt.isec.amov.cityfix.viewmodel.NewOccurrenceViewModelFactory
import pt.isec.amov.cityfix.viewmodel.OccurrencesViewModel
import pt.isec.amov.cityfix.viewmodel.OperatorViewModel
import pt.isec.amov.cityfix.viewmodel.OperatorViewModelFactory
import pt.isec.amov.cityfix.viewmodel.ProfileViewModel
import pt.isec.amov.cityfix.viewmodel.ProfileViewModelFactory
import pt.isec.amov.cityfix.viewmodel.AuthViewModel
import pt.isec.amov.cityfix.viewmodel.AuthViewModelFactory

enum class Profile(val label: String){
    ANONYMOUS("Anónimo"),
    CITIZEN("Munícipe"),
    OPERATOR("Operador"),
}

// o city fix tem muitos ecrãs por isso é melhor que seja uma função ao invés de uma classe
@Composable
fun CityFixNavHost(
    repository: OccurrenceRepository,
    userRepository: UserRepository,
    categoryRepository: CategoryRepository,
    authRepository: AuthRepository,
    navController: NavHostController,
    viewModel: OccurrencesViewModel,
    currentProfile: Profile,
    onAuthenticated: (AuthUser) -> Unit,
    onContinueAsAnonymous: () -> Unit = {},
    onImpactDetectionChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
){
    val appContext = LocalContext.current.applicationContext
    NavHost(navController = navController,
        startDestination = LoginRoute,
        modifier = modifier
    ) {
        composable<LoginRoute> { backStack ->
            val authViewModel: AuthViewModel = viewModel(
                viewModelStoreOwner = backStack,
                factory = AuthViewModelFactory(appContext, authRepository, userRepository)
            )
            val state by authViewModel.uiState.collectAsStateWithLifecycle()

            LoginScreen(
                state = state,
                onNameChange = authViewModel::onNameChanged,
                onEmailChange = authViewModel::onEmailChanged,
                onPhoneChange = authViewModel::onPhoneChanged,
                onNifChange = authViewModel::onNifChanged,
                onAddressChange = authViewModel::onAddressChanged,
                onLocationSelected = authViewModel::onLocationSelected,
                onPasswordChange = authViewModel::onPasswordChanged,
                onConfirmPasswordChange = authViewModel::onConfirmPasswordChanged,
                onModeChange = authViewModel::onModeChanged,
                onSubmit = authViewModel::submit,
                onAuthenticated = onAuthenticated,
                onContinueAsAnonymous = onContinueAsAnonymous
            )
        }
        composable<OccurrenceListRoute>{
            val occurrences by viewModel.occurrencesList.collectAsStateWithLifecycle()
            val categories by produceState<CategoriesList?>(initialValue = null) {
                value = categoryRepository.loadCategories()
            }
            val visibleOccurrences = remember(occurrences, currentProfile){
                when(currentProfile){
                    Profile.ANONYMOUS -> occurrences.filter {
                        it.range == Visibility.PUBLIC &&
                                it.status != TicketStatus.RESOLVIDO &&
                                it.status != TicketStatus.ENCERRADO &&
                                DistanceUtils.isWithinAnonymousRadius(it.location)
                    }
                    Profile.CITIZEN -> {
                        val uid = authRepository.currentUser?.id.orEmpty()
                        occurrences.filter {
                            it.authorId == uid || uid in it.followersIds
                        }
                    }
                    Profile.OPERATOR -> occurrences
                }
            }
            OccurrencesListScreen(
                occurrences = visibleOccurrences,
                categoryNameFor = { id -> categories?.findById(id)?.title ?: id },
                showStatus = currentProfile != Profile.ANONYMOUS,
                onClick = { id ->
                    navController.navigate(OccurrenceDetailsRoute(id))
                }
            )
        }
        composable<OccurrenceDetailsRoute>{ backstack -> // backstack é uma pilha que controla a ordem das telas e é Last In First Out
            val route = backstack.toRoute<OccurrenceDetailsRoute>()
            val occurrences by viewModel.occurrencesList.collectAsStateWithLifecycle()
            val messagesByOccurrence by viewModel.messagesByOccurrence.collectAsStateWithLifecycle()
            val occurrence = remember(occurrences, route.occurrenceId) {
                    occurrences.find { it.id == route.occurrenceId }
            }
            val author by produceState<User?>(initialValue = null, occurrence?.authorId) {
                value = occurrence?.authorId?.let { userRepository.loadUsers().findById(it) }
            }
            val categoryName by produceState(
                initialValue = occurrence?.categoryId ?: "",
                occurrence?.categoryId
            ) {
                value = occurrence?.categoryId?.let {
                    categoryRepository.loadCategories().findById(it)?.title ?: it
                } ?: ""
            }
            OccurrenceDetailScreen(
                occurrence = occurrence,
                currentProfile = currentProfile,
                categoryName = categoryName,
                author = author,
                canReopen = occurrence?.let { viewModel.canReopen(it) } ?: false,
                onResolve = { images, videos ->
                    occurrence?.let { viewModel.resolveOccurrence(it.id, images, videos) }
                },
                onChangeStatus = { newStatus ->
                    occurrence?.let { viewModel.changeStatus(it.id, newStatus) }
                },
                onReopen = { justification ->
                    occurrence?.let { viewModel.reopenOccurrence(it.id, justification) }
                },
                messages = messagesByOccurrence[route.occurrenceId].orEmpty(),
                onSendMessage = { text ->
                    val senderRole = when (currentProfile) {
                        Profile.ANONYMOUS -> null
                        Profile.CITIZEN -> UserRole.MUNICIPE
                        Profile.OPERATOR -> UserRole.OPERADOR
                    }
                    senderRole?.let { role ->
                        val senderId = if (role == UserRole.MUNICIPE) {
                            occurrence?.authorId.orEmpty()
                        } else {
                            "operator"
                        }
                        viewModel.sendMessage(route.occurrenceId, senderId, role, text)
                    }
                }
            )
        }
        composable<EditOccurrenceRoute>{ backStack->
            val route = backStack.toRoute<EditOccurrenceRoute>()
            val newViewModel: NewOccurrenceViewModel = viewModel(
                viewModelStoreOwner = backStack,
                factory = NewOccurrenceViewModelFactory(appContext, repository, categoryRepository, route.occurrenceId)
            )
            val state by newViewModel.uiState.collectAsStateWithLifecycle()

            NewOccurrenceScreen(
                state = state,
                onCategorySelected = newViewModel::onCategorySelected,
                onSituationSelected = newViewModel::onSituationSelected,
                onDescriptionChange = newViewModel::onDescriptionChanged,
                onVisibilitySelected = newViewModel::onVisibilitySelected,
                onImagesAdded = newViewModel::onImagesAdded,
                onImageRemoved = newViewModel::onImageRemoved,
                onVideoAdded = newViewModel::onVideoAdded,
                onVideoRemoved = newViewModel::onVideoRemoved,
                onSaveClick = {
                    val uid = authRepository.currentUser?.id.orEmpty()
                    newViewModel.saveOccurrence(authorId = uid) {
                        viewModel.refreshOccurrences()
                        navController.popBackStack()
                    }
                },
                onAddressChange = newViewModel::onAddressChanged,
                onLocationSelected = newViewModel::onLocationSelected,
            )
        }
        composable<OperatorCategoriesRoute> { backStack ->
            val operatorViewModel: OperatorViewModel = viewModel(
                viewModelStoreOwner = backStack,
                factory = OperatorViewModelFactory(appContext, userRepository, categoryRepository)
            )
            val state by operatorViewModel.categoriesUiState.collectAsStateWithLifecycle()

            OperatorCategoriesScreen(
                state = state,
                onRefresh = operatorViewModel::refreshCategories,
                onSaveCategory = operatorViewModel::saveCategory,
                onSetCategoryActive = operatorViewModel::setCategoryActive
            )
        }
        composable<CitizenValidationRoute> { backStack ->
            val operatorViewModel: OperatorViewModel = viewModel(
                viewModelStoreOwner = backStack,
                factory = OperatorViewModelFactory(appContext, userRepository, categoryRepository)
            )
            val state by operatorViewModel.uiState.collectAsStateWithLifecycle()

            CitizenValidationScreen(
                state = state,
                onApproveCitizen = operatorViewModel::approveCitizen,
                onRefresh = operatorViewModel::refreshPendingCitizens
            )
        }
        composable<DashboardRoute>{
            val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
            DashboardScreen(metrics = metrics)
        }
        composable<RegisterRoute> { backStack ->
            val profileViewModel: ProfileViewModel = viewModel(
                viewModelStoreOwner = backStack,
                factory = ProfileViewModelFactory(appContext, userRepository, userId = null)
            )
            val state by profileViewModel.uiState.collectAsStateWithLifecycle()

            RegisterScreen(
                state = state,
                onNameChange = profileViewModel::onNameChanged,
                onEmailChange = profileViewModel::onEmailChanged,
                onPhoneChange = profileViewModel::onPhoneChanged,
                onNifChange = profileViewModel::onNifChanged,
                onPictureChange = profileViewModel::onPicturePathChanged,
                onImpactDetectionChange = profileViewModel::onImpactDetectionChanged,
                onRegisterClick = {
                    profileViewModel.saveProfile {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable<ProfileRoute> { backStack ->
            val route = backStack.toRoute<ProfileRoute>()
            val profileViewModel: ProfileViewModel = viewModel(
                viewModelStoreOwner = backStack,
                factory = ProfileViewModelFactory(appContext, userRepository, userId = route.userId)
            )
            val state by profileViewModel.uiState.collectAsStateWithLifecycle()

            ProfileScreen(
                state = state,
                onNameChange = profileViewModel::onNameChanged,
                onEmailChange = profileViewModel::onEmailChanged,
                onPhoneChange = profileViewModel::onPhoneChanged,
                onNifChange = profileViewModel::onNifChanged,
                onPictureChange = profileViewModel::onPicturePathChanged,
                onImpactDetectionChange = { enabled ->
                    profileViewModel.onImpactDetectionChanged(enabled)
                    onImpactDetectionChanged(enabled)
                },
                onLocationSelected = profileViewModel::onLocationSelected,
                onSaveClick = {
                    profileViewModel.saveProfile()
                }
            )
        }
    }
}
