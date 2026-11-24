package pt.isec.amov.cityfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.UserRepository

class ProfileViewModelFactory(
    private val context: Context,
    private val repository: UserRepository,
    private val userId: String? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(context.applicationContext, repository, userId) as T
    }
}
