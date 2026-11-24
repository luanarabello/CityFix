package pt.isec.amov.cityfix.utils

import pt.isec.amov.cityfix.model.User

object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9+._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val phoneRegex = Regex("^\\d{9}$")
    private val nifRegex = Regex("^\\d{9}$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun isValidPhone(phone: String): Boolean = phoneRegex.matches(phone.trim())

    fun isValidNifFormat(nif: String): Boolean = nifRegex.matches(nif.trim())

    fun isNifUnique(
        nif: String,
        users: List<User>,
        userIdToIgnore: String? = null
    ): Boolean = users.none { user ->
        user.nif == nif.trim() && user.id != userIdToIgnore
    }
}
