package pt.isec.amov.cityfix.model

import java.text.Collator
import java.util.Locale

class UsersList(initialUsers: List<User> = emptyList()) {
    private val _users = initialUsers.toMutableList()

    fun getAllSorted(): List<User> {
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
        return _users.sortedWith { first, second ->
            collator.compare(first.name.trim(), second.name.trim())
        }
    }

    fun findById(id: String): User? = _users.find { it.id == id }

    fun add(user: User) {
        _users.add(user)
    }

    fun upsert(user: User) {
        val index = _users.indexOfFirst { it.id == user.id }
        if (index >= 0) {
            _users[index] = user
        } else {
            _users.add(user)
        }
    }

    fun rawList(): List<User> = _users.toList()
}
