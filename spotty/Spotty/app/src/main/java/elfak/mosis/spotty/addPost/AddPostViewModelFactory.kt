package elfak.mosis.spotty.addPost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AddPostViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddPostViewModel::class.java)) {
            return AddPostViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
