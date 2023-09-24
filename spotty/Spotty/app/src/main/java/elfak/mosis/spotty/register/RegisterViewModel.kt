package elfak.mosis.spotty.register

import android.graphics.Bitmap
import android.net.Uri
import android.util.Patterns
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import elfak.mosis.spotty.R
import elfak.mosis.spotty.data.User
import java.io.ByteArrayOutputStream
import id.zelory.compressor.Compressor
import java.io.File

class RegisterViewModel: ViewModel() {
    private val _registerForm = MutableLiveData<RegisterFormState>()
    val registerFormState: LiveData<RegisterFormState> = _registerForm

    fun uploadImageToStorage(picture: Uri) : UploadTask{
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val imageRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")
        return imageRef.putFile(picture)
    }
    fun uploadImageToStorage(picture: Bitmap):UploadTask {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val imageRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")
        val baos = ByteArrayOutputStream()
        picture.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()
        return imageRef.putBytes(imageData)
    }
    fun register(email: String, password: String, confPassword:String,
                 firstname:String, lastname:String, phone: String, picture: Uri, callback:IRegister){
        uploadImageToStorage(picture).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { it ->
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { a: Task<AuthResult>? ->
                        if (a?.isSuccessful!!) {
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            val user =
                                User(email, firstname, userId!!, it.toString(), lastname, phone, 0,
                                    hashMapOf<String,Boolean>())
                            FirebaseFirestore.getInstance().collection("users").add(user)
                                .addOnCompleteListener { firestoreTask ->
                                    if (firestoreTask.isSuccessful) {
                                        callback.onSuccess()
                                    } else {
                                        callback.onFail()
                                    }
                                }
                                .addOnFailureListener { it ->
                                    print(it.message)
                                }
                        }
                    }
            }.addOnFailureListener() { it ->
                print(it.message)
            }
        }
    }
    fun register(email: String, password: String, confPassword:String,
                 firstname:String, lastname:String, phone: String, picture: Bitmap, callback:IRegister){

        uploadImageToStorage(picture).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { it ->
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { a: Task<AuthResult>? ->
                        if (a?.isSuccessful!!) {
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            val user =
                                User(email, firstname, userId!!, it.toString(), lastname, phone, 0,
                                    hashMapOf<String,Boolean>())
                            FirebaseFirestore.getInstance().collection("users").add(user)
                                .addOnCompleteListener { firestoreTask ->
                                    if (firestoreTask.isSuccessful) {
                                        callback.onSuccess()
                                    } else {
                                        callback.onFail()
                                    }
                                }
                                .addOnFailureListener { it ->
                                    print(it.message)
                                }
                        }
                    }
            }.addOnFailureListener() { it ->
                print(it.message)
            }
        }
    }

    fun registerDataChanged(email: String, password: String, confPassword:String,
                            firstname:String, lastname:String, phone: String) {
        if(!isNameValid(firstname)){
            _registerForm.value = RegisterFormState(firstnameError = R.string.invalid_first_name)
        }else if(!isNameValid(lastname)) {
            _registerForm.value = RegisterFormState(lastnameError = R.string.invalid_last_name)
        }else if (!isUserNameValid(email)) {
            _registerForm.value = RegisterFormState(usernameError = R.string.invalid_username)
        } else if(!isPhoneValid(phone)){
                _registerForm.value = RegisterFormState(confpassError = R.string.invalid_phone)
        } else if (!isPasswordValid(password)) {
            _registerForm.value = RegisterFormState(passwordError = R.string.invalid_password)
        }else if (!isConfirmPasswordValid(password,confPassword)) {
            _registerForm.value = RegisterFormState(confpassError = R.string.invalid_conf_password)
        }else {
            _registerForm.value = RegisterFormState(isDataValid = true)
        }
    }
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains("@")) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            false
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    private fun isNameValid(name:String):Boolean{
        return name.length > 1
    }
    private fun isPhoneValid(phone:String):Boolean{
        return Patterns.PHONE.matcher(phone).matches()
    }

    private fun isConfirmPasswordValid(password:String, confpass:String):Boolean{
        return password==confpass
    }

}