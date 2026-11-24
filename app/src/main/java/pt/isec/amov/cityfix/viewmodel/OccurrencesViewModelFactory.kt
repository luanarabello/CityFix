package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.OccurrenceRepository

class OccurrencesViewModelFactory(private val repository: OccurrenceRepository)
    : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T{
        return OccurrencesViewModel(repository) as T
    }
}