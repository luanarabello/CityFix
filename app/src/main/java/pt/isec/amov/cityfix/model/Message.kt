package pt.isec.amov.cityfix.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Message (
    val id: String = UUID.randomUUID().toString(),
    val occurrenceId: String = "",
    val senderId: String = "",
    val senderRole: UserRole = UserRole.MUNICIPE,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()

)
/*
   * NOTE
   System.currentTimeMillis() uses the phone's clock.
   * If the citizen's and the operator's phones are a few minutes apart,
   * messages could appear out of order. Firestore can stamp the server's
   * time instead (FieldValue.serverTimestamp()). Ignore this for now while
   * the data is local; it's just something to know for later.
   * */