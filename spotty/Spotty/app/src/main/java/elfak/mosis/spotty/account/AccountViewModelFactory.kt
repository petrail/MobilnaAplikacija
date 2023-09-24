package elfak.mosis.spotty.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AccountViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            return AccountViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
