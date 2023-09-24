package elfak.mosis.spotty.addPost

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import elfak.mosis.spotty.R
import elfak.mosis.spotty.data.PostFirestore
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class AddPostViewModel : ViewModel() {
    private val _postForm = MutableLiveData<PostFormState>()
    val postForm: LiveData<PostFormState> = _postForm
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
    fun addPost(picture: Bitmap,postName:String, postDesc:String, currentLoc:LatLng, postType:Long, callback:IAddPost) {
        uploadImageToStorage(picture).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { it ->
                FirebaseFirestore.getInstance().collection("users")
                    .where(Filter.equalTo("id", FirebaseAuth.getInstance().uid))
                    .get().addOnSuccessListener { docs ->
                        for (doc in docs) {
                            var fname = doc["firstName"].toString()
                            var lname = doc["lastName"].toString()
                            var author = hashMapOf<String,String>("firstName" to fname, "lastName" to lname)
                            var p = PostFirestore(
                                FirebaseAuth.getInstance().uid.toString(),
                                author,
                                0,
                                GeoPoint(currentLoc.latitude,currentLoc.longitude),
                                it.toString(),
                                postName,
                                LocalDate.now().toString(),
                                postDesc,
                                postType,
                                0
                            )

                            FirebaseFirestore.getInstance().collection("posts").add(p)
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
            }
            }.addOnFailureListener() { it ->
                print(it.message)
            }
    }
    fun addPost(picture: Uri,postName:String, postDesc:String, currentLoc:LatLng, postType:Long, callback:IAddPost) {
        uploadImageToStorage(picture).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { it ->
                FirebaseFirestore.getInstance().collection("users")
                    .where(Filter.equalTo("id", FirebaseAuth.getInstance().uid))
                    .get().addOnSuccessListener { docs ->
                        for (doc in docs) {
                            var fname = doc["firstName"].toString()
                            var lname = doc["lastName"].toString()
                            var author = hashMapOf<String,String>("firstName" to fname, "lastName" to lname)
                            var p = PostFirestore(
                                FirebaseAuth.getInstance().uid.toString(),
                                author,
                                0,
                                GeoPoint(currentLoc.latitude,currentLoc.longitude),
                                it.toString(),
                                postName,
                                LocalDate.now().toString(),
                                postDesc,
                                postType,
                                0
                            )

                            FirebaseFirestore.getInstance().collection("posts").add(p)
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
            }
        }.addOnFailureListener() { it ->
            print(it.message)
        }
    }

    fun registerDataChanged(postName: String, postDesc: String) {
        if(!isTextValid(postName)){
            _postForm.value = PostFormState(postNameError = R.string.invalid_first_name)
        }else if(!isTextValid(postDesc)){
            _postForm.value = PostFormState(postDescError = R.string.invalid_first_name)
        }else {
            _postForm.value = PostFormState(isDataValid = true)
        }
    }
    private fun isTextValid(text:String):Boolean{
        return text.length > 1
    }
}