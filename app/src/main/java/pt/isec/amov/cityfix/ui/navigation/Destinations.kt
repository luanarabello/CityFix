package pt.isec.amov.cityfix.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OccurrenceListRoute

@Serializable
data class OccurrenceDetailsRoute(val occurrenceId: String)

@Serializable
data class EditOccurrenceRoute(val occurrenceId: String? = null)

@Serializable
data object DashboardRoute

@Serializable
data object RegisterRoute

@Serializable
data object LoginRoute

@Serializable
data class ProfileRoute(val userId: String)

@Serializable
data object OperatorCategoriesRoute

@Serializable
data object CitizenValidationRoute
