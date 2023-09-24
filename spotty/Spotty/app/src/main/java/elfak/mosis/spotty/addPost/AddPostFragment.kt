package elfak.mosis.spotty.addPost

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import elfak.mosis.spotty.IMainActivity
import elfak.mosis.spotty.R
import elfak.mosis.spotty.databinding.FragmentAddPostBinding
import elfak.mosis.spotty.databinding.FragmentRegisterBinding
import elfak.mosis.spotty.register.IRegister
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

interface  IAddPost{
    fun onSuccess()
    fun onFail()
    fun onCompressUriOver()
    fun onCompressBitOver()
}
class AddPostFragment(_callback: IMainActivity) : Fragment(), IAddPost {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val callback:IAddPost = this
    private var mainActCallback=_callback

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddPostViewModel
    private var imageUri: Uri? = null
    private var imageBitmap: Bitmap? = null

    private val GALLERY_REQUEST_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val LOCATION_PERMISSION_REQUEST_CODE=123

    private lateinit var locationProvider: FusedLocationProviderClient

    private lateinit var currentLoc:LatLng

    private var placeType:Long = 3
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        binding.fromdevicePost.setOnClickListener{
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }

        binding.fromcameraPost.setOnClickListener{
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        }
        binding.caffeRB.setOnClickListener{
            placeType=0
        }
        binding.hotelRB.setOnClickListener{
            placeType=1
        }
        binding.restRB.setOnClickListener{
            placeType=2
        }
        binding.otherRB.setOnClickListener{
            placeType=3
        }
        return binding.root
    }

    private fun getFileFromUri(contentResolver: ContentResolver, uri: Uri): File? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)

        return cursor?.use { c ->
            val columnIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            c.moveToFirst()
            val filePath = c.getString(columnIndex)
            File(filePath)
        }
    }
    suspend fun compressImage(uri: Uri, callback: IAddPost){
        val contentResolver: ContentResolver = context?.contentResolver!!
        val originalFile: File? = getFileFromUri(contentResolver, uri)
        val compressed = Compressor.compress(requireContext(),originalFile!!){
            resolution(256, 256)
            quality(80)
        }
        imageUri = Uri.fromFile(compressed)
        callback.onCompressUriOver()
    }
    fun compressImage(bitmap: Bitmap,callback: IAddPost){

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256,256, false)
        val outputStream = ByteArrayOutputStream()

        val format = Bitmap.CompressFormat.JPEG
        val quality = 80

        resizedBitmap.compress(format, quality, outputStream)

        val compressedImageData = outputStream.toByteArray()

        imageBitmap = BitmapFactory.decodeByteArray(compressedImageData, 0, compressedImageData.size)

        outputStream.close()
        callback.onCompressBitOver()
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AddPostViewModel::class.java)
        locationProvider = LocationServices.getFusedLocationProviderClient(requireActivity());
        // TODO: Use the ViewModel
        binding.addPost.setOnClickListener {
            addPost()
        }
        viewModel.postForm.observe(viewLifecycleOwner,
            Observer { postForm ->
                if (postForm == null) {
                    return@Observer
                }
                binding.addPost.isEnabled = postForm.isDataValid
                postForm.postNameError?.let {
                    binding.postNameEdit.error = getString(it)
                }
                postForm.postDescError?.let {
                    binding.postDescEdit.error = getString(it)
                }
            })
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                viewModel.registerDataChanged(
                    binding.postNameEdit.text.toString(),
                    binding.postDescEdit.text.toString(),
                )
            }
        }
        binding.postNameEdit.addTextChangedListener(afterTextChangedListener)
        binding.postDescEdit.addTextChangedListener(afterTextChangedListener)
    }

    private fun addPost(){
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                locationPermissions,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        else {
            binding.loading.visibility= View.VISIBLE
            locationProvider.getLastLocation().addOnSuccessListener { loc ->
                currentLoc= LatLng(loc.latitude,loc.longitude)
                if(imageUri==null) {
                    viewModel.addPost(
                        imageBitmap!!,
                        binding.postNameEdit.text.toString(),
                        binding.postDescEdit.text.toString(),
                        currentLoc,
                        placeType,
                        this
                    )
                }
                else{
                    viewModel.addPost(
                        imageUri!!,
                        binding.postNameEdit.text.toString(),
                        binding.postDescEdit.text.toString(),
                        currentLoc,
                        placeType,
                        this
                    )
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    imageBitmap = null
                    imageUri = data?.data

                    binding.slika.visibility = View.VISIBLE
                    binding.addPost.isEnabled=false
                    binding.errorMsgPost.text = resources.getText(R.string.load_photo)
                    coroutineScope.launch {
                        compressImage(imageUri!!,callback)
                    }
                    //binding.slika.setImageURI(imageUri)
                    //binding.addPost.isEnabled=true

                }
                CAMERA_REQUEST_CODE -> {
                    imageUri = null
                    binding.slika.visibility = View.VISIBLE
                    binding.addPost.isEnabled=false
                    binding.errorMsgPost.text = resources.getText(R.string.load_photo)
                    coroutineScope.launch {
                        compressImage(data?.extras?.get("data") as Bitmap,callback)
                    }

                }
            }
        }
        else{
            binding.slika.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_account,requireContext().theme))
            binding.addPost.isEnabled=false
        }
    }
    override fun onSuccess() {
        binding.loading.visibility= View.GONE
        mainActCallback.backToMain()
    }

    override fun onFail() {
        TODO("Not yet implemented")
    }
    override fun onCompressUriOver() {
        requireActivity().runOnUiThread{
            binding.errorMsgPost.text = resources.getText(R.string.valid_photo)
            binding.slika.setImageURI(imageUri)
            binding.errorMsgPost.setTextColor(resources.getColor(R.color.green))
            binding.addPost.isEnabled=true
        }
    }

    override fun onCompressBitOver() {
        requireActivity().runOnUiThread {
            binding.errorMsgPost.text = resources.getText(R.string.valid_photo)
            binding.slika.setImageBitmap(imageBitmap)
            binding.errorMsgPost.setTextColor(resources.getColor(R.color.green))
            binding.addPost.isEnabled = true
        }
    }

}