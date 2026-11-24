package pt.isec.amov.cityfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.CategoryRepository
import pt.isec.amov.cityfix.data.OccurrenceRepository

class NewOccurrenceViewModelFactory(
    private val context: Context,
    private val repository: OccurrenceRepository,
    private val categoryRepository: CategoryRepository,
    private val occurrenceId: String? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewOccurrenceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewOccurrenceViewModel(context.applicationContext, repository, categoryRepository, occurrenceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
