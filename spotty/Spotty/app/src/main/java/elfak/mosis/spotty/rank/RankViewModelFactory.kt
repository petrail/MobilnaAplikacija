package elfak.mosis.spotty.rank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RankViewModelFactory: ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RankViewModel::class.java)) {
            return RankViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}