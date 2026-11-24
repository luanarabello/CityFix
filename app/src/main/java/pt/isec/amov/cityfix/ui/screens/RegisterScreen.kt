package pt.isec.amov.cityfix.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pt.isec.amov.cityfix.viewmodel.ProfileUiState

@Composable
fun RegisterScreen(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNifChange: (String) -> Unit,
    onPictureChange: (String?) -> Unit,
    onImpactDetectionChange: (Boolean) -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UserProfileForm(
        title = "Register",
        submitLabel = "Create account",
        state = state,
        onNameChange = onNameChange,
        onEmailChange = onEmailChange,
        onPhoneChange = onPhoneChange,
        onNifChange = onNifChange,
        onPictureChange = onPictureChange,
        onImpactDetectionChange = onImpactDetectionChange,
        onSubmitClick = onRegisterClick,
        modifier = modifier
    )
}
