package elfak.mosis.spotty.account

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Patterns
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import elfak.mosis.spotty.IMainActivity
import elfak.mosis.spotty.LoginRegisterActivity
import elfak.mosis.spotty.R
import elfak.mosis.spotty.data.DownloadImageTask
import elfak.mosis.spotty.login.LoginFormState
import elfak.mosis.spotty.register.RegisterFormState
import org.w3c.dom.Text
import java.io.ByteArrayOutputStream

class AccountViewModel : ViewModel() {
    // TODO: Implement the ViewModel
    private var _phone = MutableLiveData<String>()
    val phone:LiveData<String>
        get() = _phone

    private var _firstname = MutableLiveData<String>()
    val firstname:LiveData<String>
        get() = _firstname

    private var _lastname = MutableLiveData<String>()
    val lastname:LiveData<String>
        get() = _lastname

    private var _email = MutableLiveData<String>()
    val email:LiveData<String>
        get() = _email

    private var _password = MutableLiveData<String>()
    val password:LiveData<String>
        get() = _password

    private var pass:String=""

    private var _imageUrl = MutableLiveData<String>()

    private var _imageUri = MutableLiveData<Uri?>()
    val imageUri:LiveData<Uri?>
        get() = _imageUri

    private var _imageBitmap = MutableLiveData<Bitmap?>()
    public val imageBitmap:LiveData<Bitmap?>
        get() = _imageBitmap

    private val _registerForm = MutableLiveData<RegisterFormState>()
    val registerFormState: LiveData<RegisterFormState> = _registerForm

    fun fetchAccountData(img: ImageView, callback: IAccount){
        Firebase.firestore.collection("users").where(Filter.equalTo("id", FirebaseAuth.getInstance().uid))
            .get().addOnSuccessListener { docs->
                for(doc in docs){
                    val firstName = doc.data["firstName"] as String
                    val lastName = doc.data["lastName"] as String
                    val email = doc.data["email"] as String
                    val phone= doc.data["phone"] as String
                    val imgUrl = doc.data["imageUrl"] as String
                    _phone.value = phone
                    _firstname.value = firstName
                    _lastname.value = lastName
                    _email.value = email
                    _imageUrl.value = imgUrl
                    registerDataChanged(email,"",firstName,lastName,phone)
                    callback.dataFetched()
                    val dTask = DownloadImageTask(img)
                    dTask.execute(imgUrl)
                }
            }
    }
    fun deleteAccount(callback:IMainActivity){
        val userID = FirebaseAuth.getInstance().uid
        val fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(_imageUrl.value!!)
        fileRef.delete().addOnSuccessListener {
            FirebaseFirestore.getInstance().collection("users")
                .where(Filter.equalTo("id",userID))
                .get()
                .addOnSuccessListener {docs->
                    for (doc in docs) {
                        doc.reference.delete().addOnSuccessListener {
                            FirebaseAuth.getInstance().currentUser!!.delete()
                                .addOnSuccessListener {
                                }.addOnFailureListener { it ->
                                    print(it.message)
                                }
                        }.addOnFailureListener { it ->
                            print(it.message)
                        }
                    }
                    callback.logout()
                }
                .addOnFailureListener { it->
                    print(it.message)
                }
        }.addOnFailureListener { it->
            print(it.message)
        }
    }
    fun setImageUri(uri:Uri?){
        _imageUri.value = uri
    }
    fun setImageBitmap(bitmap: Bitmap?){
        _imageBitmap.value = bitmap
    }

    private fun saveNewData(){
        val updates = hashMapOf(
            "firstName" to firstname.value,
            "lastName" to lastname.value,
            "phone" to phone.value,
            "email" to email.value,
            "imageUrl" to _imageUrl.value
            // Add other fields you want to update
        )
        FirebaseFirestore.getInstance().collection("users").where(Filter.equalTo("id",FirebaseAuth.getInstance().uid!!))
            .get().addOnSuccessListener { docs->
                for(doc in docs){
                    doc.reference.update(updates as Map<String, Any>).addOnFailureListener {e->
                        print(e.message)
                    }
                }
            }

    }
    fun saveData(firstnameText: String,lastnameText: String,emailText: String,passwordText: String,phoneText: String){
        _firstname.value = firstnameText
        _lastname.value = lastnameText
        _email.value = emailText
        _password.value = passwordText
        _phone.value = phoneText
        val changedPassword = !(_password.value?.isEmpty())!!
        val changedImage = !(imageUri.value==null && imageBitmap.value==null)
        if(changedPassword){
            //Ako je promenio sifru, sacuvaj je na firebase auth
            FirebaseAuth.getInstance().currentUser?.updatePassword(_password.value!!)
        }
        if(changedImage){
            //Ako se menja slika, obrisi staru sa baze, uploaduj novu i nadji njen url
            val storageRef = FirebaseStorage.getInstance().reference
            var fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(_imageUrl.value!!)

            fileRef.delete().addOnSuccessListener {
                var h: UploadTask? = null
                if(imageUri.value!=null){
                    h = uploadImageToStorage(imageUri.value!!)
                }
                else if(imageBitmap.value!=null){
                    h = uploadImageToStorage(imageBitmap.value!!)
                }
                h?.addOnSuccessListener { taskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { it ->
                            _imageUrl.value = it.toString()
                            saveNewData()
                        }
                    }
                    ?.addOnFailureListener{e->
                        print(e.message)
                    }
            }.addOnFailureListener { e->
                print(e.message)
            }
        }
        else{
            saveNewData()
        }
        // Sacuvaj nove podatke
    }
    fun uploadImageToStorage(picture: Uri) : UploadTask {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val imageRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")
        return imageRef.putFile(picture)
    }
    fun uploadImageToStorage(picture: Bitmap): UploadTask {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val imageRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")
        val baos = ByteArrayOutputStream()
        picture.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()
        return imageRef.putBytes(imageData)
    }

    fun registerDataChanged(email: String, password: String,
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
        return password.length > 5 || password.isEmpty()
    }

    private fun isNameValid(name:String):Boolean{
        return name.length > 1
    }
    private fun isPhoneValid(phone:String):Boolean{
        return Patterns.PHONE.matcher(phone).matches()
    }

}