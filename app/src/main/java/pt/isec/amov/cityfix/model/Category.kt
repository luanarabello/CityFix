package pt.isec.amov.cityfix.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Category (
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val active: Boolean = true,
)