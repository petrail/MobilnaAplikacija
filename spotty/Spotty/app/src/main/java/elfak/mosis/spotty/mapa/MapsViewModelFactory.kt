package elfak.mosis.spotty.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MapsViewModelFactory: ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
            return MapsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}