package elfak.mosis.spotty.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import elfak.mosis.spotty.login.LoginViewModel

class RegisterViewModelFactory: ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}