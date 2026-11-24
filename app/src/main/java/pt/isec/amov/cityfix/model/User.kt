package pt.isec.amov.cityfix.model


import kotlinx.serialization.Serializable
import java.util.UUID

enum class UserRole{
    MUNICIPE, OPERADOR
}

@Serializable
data class User (
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val nif: String = "",
    val profileType: UserRole = UserRole.MUNICIPE,
    val picturePath: String? = null,
    val location: GeoLocation? = null,
    val impactDetection: Boolean = false,
    val active: Boolean = false // active or pending

)