package elfak.mosis.spotty.account

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import elfak.mosis.spotty.IMainActivity
import elfak.mosis.spotty.R
import elfak.mosis.spotty.databinding.FragmentAccountBinding
import elfak.mosis.spotty.databinding.FragmentRegisterBinding
import elfak.mosis.spotty.register.RegisterFormState
import elfak.mosis.spotty.register.RegisterViewModel
import elfak.mosis.spotty.register.RegisterViewModelFactory
import java.io.ByteArrayOutputStream

interface IAccount{
    fun dataFetched()
}
class AccountFragment   (_callback:IMainActivity) : Fragment(),IAccount {
    private var callback = _callback
    private lateinit var accountViewModel: AccountViewModel
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AccountViewModel
    private val GALLERY_REQUEST_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        binding.fromdeviceEdit.setOnClickListener{
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }

        binding.fromcameraEdit.setOnClickListener{
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    if(data?.data==null) return
                    viewModel.setImageUri(data?.data)
                    viewModel.setImageBitmap(null)
                    binding.accountImage.setImageURI(data?.data)
                }
                CAMERA_REQUEST_CODE -> {
                    if(data?.extras?.get("data") as Bitmap == null) return
                    viewModel.setImageUri(null)
                    viewModel.setImageBitmap(data?.extras?.get("data") as Bitmap)
                    binding.accountImage.setImageBitmap(data?.extras?.get("data") as Bitmap)
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AccountViewModel::class.java)
        // TODO: Use the ViewModel
        viewModel.phone.observe(viewLifecycleOwner) { it ->
            binding.phoneEdit.setText(it)
        }
        viewModel.firstname.observe(viewLifecycleOwner) { it ->
            binding.firstnameEdit.setText(it)
        }
        viewModel.lastname.observe(viewLifecycleOwner) { it ->
            binding.lastnameEdit.setText(it)
        }
        viewModel.email.observe(viewLifecycleOwner) { it ->
            binding.emailEdit.setText(it)
        }
        viewModel.password.observe(viewLifecycleOwner) { it ->
            binding.passwordEdit.setText(it)
        }
        binding.deleteBtn.setOnClickListener {
            viewModel.deleteAccount(callback)
        }
        binding.saveBtn.setOnClickListener {
            viewModel.saveData(binding.firstnameEdit.text.toString(),
                                binding.lastnameEdit.text.toString(),
                                binding.emailEdit.text.toString(),
                                binding.passwordEdit.text.toString(),
                                binding.phoneEdit.text.toString())
        }
        viewModel.setImageBitmap(null)
        viewModel.setImageUri(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountViewModel = ViewModelProvider(this, AccountViewModelFactory())
            .get(AccountViewModel::class.java)

        accountViewModel.registerFormState.observe(viewLifecycleOwner,
            Observer { registerFormState ->
                if (registerFormState == null) {
                    return@Observer
                }
                binding.saveBtn.isEnabled = registerFormState.isDataValid
                registerFormState.usernameError?.let {
                    binding.emailEdit.error = getString(it)
                }
                registerFormState.passwordError?.let {
                    binding.passwordEdit.error = getString(it)
                }
                registerFormState.phoneError?.let{
                    binding.phoneEdit.error = getString(it)
                }
                registerFormState.firstnameError?.let{
                    binding.firstnameEdit.error = getString(it)
                }
                registerFormState.lastnameError?.let{
                    binding.lastnameEdit.error = getString(it)
                }
            })

        accountViewModel.fetchAccountData(binding.accountImage,this)

    }

    override fun dataFetched() {
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                accountViewModel.registerDataChanged(
                    binding.emailEdit.text.toString(),
                    binding.passwordEdit.text.toString(),
                    binding.firstnameEdit.text.toString(),
                    binding.lastnameEdit.text.toString(),
                    binding.phoneEdit.text.toString()
                )
            }
        }
        binding.firstnameEdit.addTextChangedListener(afterTextChangedListener)
        binding.lastnameEdit.addTextChangedListener(afterTextChangedListener)
        binding.passwordEdit.addTextChangedListener(afterTextChangedListener)
        binding.emailEdit.addTextChangedListener(afterTextChangedListener)
        binding.phoneEdit.addTextChangedListener(afterTextChangedListener)
    }


}