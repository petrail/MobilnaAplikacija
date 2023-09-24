package elfak.mosis.spotty.register

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings.Global
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import elfak.mosis.spotty.R
import elfak.mosis.spotty.databinding.FragmentRegisterBinding
import elfak.mosis.spotty.login.LoginFragment
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File

interface IRegister{
    fun onSuccess()
    fun onFail()
    fun onCompressUriOver()
    fun onCompressBitOver()
}
class RegisterFragment : Fragment(), IRegister {
    private val coroutineScope = CoroutineScope(Dispatchers.IO) //Nit za ucitavanje slike

    private lateinit var registerViewModel: RegisterViewModel
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private var imageUri:Uri? = null // Referenca na sliku sa uredjaja
    private var imageBitmap: Bitmap? = null //Referenca na sliku iz kamere

    private val GALLERY_REQUEST_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002

    private val callback:IRegister = this
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        val view = binding.root

        var login_button = view.findViewById(R.id.login) as Button
        login_button.setOnClickListener{
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                .replace(R.id.fragmentContainerView,LoginFragment())
                .commit()
        }

        binding.fromdevice.setOnClickListener{
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }

        binding.fromcamera.setOnClickListener{
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        }

        return view
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
    suspend fun compressImage(uri: Uri, callback:IRegister){
        val contentResolver: ContentResolver = context?.contentResolver!!
        val originalFile: File? = getFileFromUri(contentResolver, uri)
        val compressed = Compressor.compress(requireContext(),originalFile!!){
            resolution(256, 256)
            quality(80)
        }
        imageUri = Uri.fromFile(compressed)
        callback.onCompressUriOver()
    }
    fun compressImage(bitmap: Bitmap,callback:IRegister){

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    imageBitmap = null
                    binding.photoFeedback.visibility = View.VISIBLE
                    binding.photoFeedback.text = resources.getText(R.string.load_photo)
                    imageUri = data?.data
                    coroutineScope.launch {
                        compressImage(imageUri!!,callback)
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    imageUri = null
                    binding.photoFeedback.visibility = View.VISIBLE
                    binding.photoFeedback.text = resources.getText(R.string.load_photo)
                    coroutineScope.launch {
                        compressImage(data?.extras?.get("data") as Bitmap,callback)
                    }
                }
            }
        }
        else{
            binding.photoFeedback.text = resources.getText(R.string.invalid_photo)
            binding.slika.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_account,requireContext().theme))
            binding.photoFeedback.setTextColor(resources.getColor(R.color.red))
            binding.register.isEnabled=false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerViewModel = ViewModelProvider(this, RegisterViewModelFactory())
            .get(RegisterViewModel::class.java)
        val firstNameText = binding.firstname
        val lastNameText = binding.lastname
        val phone = binding.phone
        val email = binding.email
        val password =binding.password
        val confPassword = binding.confpassword
        val loadingProgressBar = binding.loading

        val registerBtn = binding.register

        registerViewModel.registerFormState.observe(viewLifecycleOwner,
            Observer { registerFormState ->
                if (registerFormState == null) {
                    return@Observer
                }
                registerBtn.isEnabled = registerFormState.isDataValid
                registerFormState.usernameError?.let {
                    email.error = getString(it)
                }
                registerFormState.passwordError?.let {
                    password.error = getString(it)
                }
                registerFormState.confpassError?.let{
                    confPassword.error=getString(it)
                }
                registerFormState.phoneError?.let{
                    phone.error=getString(it)
                }
                registerFormState.firstnameError?.let{
                    firstNameText.error=getString(it)
                }
                registerFormState.lastnameError?.let{
                    lastNameText.error=getString(it)
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
                registerViewModel.registerDataChanged(
                    email.text.toString(),
                    password.text.toString(),
                    confPassword.text.toString(),
                    firstNameText.text.toString(),
                    lastNameText.text.toString(),
                    phone.text.toString()
                )
            }
        }
        firstNameText.addTextChangedListener(afterTextChangedListener)
        lastNameText.addTextChangedListener(afterTextChangedListener)
        phone.addTextChangedListener(afterTextChangedListener)
        email.addTextChangedListener(afterTextChangedListener)
        password.addTextChangedListener(afterTextChangedListener)
        confPassword.addTextChangedListener(afterTextChangedListener)

        registerBtn.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            if(imageUri !=null){
                registerViewModel.register(
                    email.text.toString(),
                    password.text.toString(),
                    confPassword.text.toString(),
                    firstNameText.text.toString(),
                    lastNameText.text.toString(),
                    phone.text.toString(),
                    imageUri!!,
                    this
                )
            }
            else if(imageBitmap!=null){
                registerViewModel.register(
                    email.text.toString(),
                    password.text.toString(),
                    confPassword.text.toString(),
                    firstNameText.text.toString(),
                    lastNameText.text.toString(),
                    phone.text.toString(),
                    imageBitmap!!,
                    this
                )
            }
            else{
                onFail()
            }
        }
    }

    private fun showRegisterFailed(@StringRes errorString: Int) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        coroutineScope.cancel()
    }

    override fun onSuccess() {
        binding.firstname.text.clear()
        binding.lastname.text.clear()
        binding.phone.text.clear()
        binding.email.text.clear()
        binding.password.text.clear()
        binding.confpassword.text.clear()
        binding.loading.visibility=View.INVISIBLE
        imageUri=null
        imageBitmap=null
        binding.photoFeedback.visibility=View.INVISIBLE
        binding.errorMsg.visibility = View.INVISIBLE
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
            .replace(R.id.fragmentContainerView,LoginFragment())
            .commit()
    }

    override fun onFail() {
        binding.loading.visibility=View.INVISIBLE
        binding.errorMsg.visibility = View.VISIBLE
    }

    override fun onCompressUriOver() {
        requireActivity().runOnUiThread{
            binding.photoFeedback.text = resources.getText(R.string.valid_photo)
            binding.slika.setImageURI(imageUri)
            binding.photoFeedback.setTextColor(resources.getColor(R.color.green))
            binding.register.isEnabled=true
        }
    }

    override fun onCompressBitOver() {
        requireActivity().runOnUiThread {
            binding.photoFeedback.text = resources.getText(R.string.valid_photo)
            binding.slika.setImageBitmap(imageBitmap)
            binding.photoFeedback.setTextColor(resources.getColor(R.color.green))
            binding.register.isEnabled = true
        }
    }
}