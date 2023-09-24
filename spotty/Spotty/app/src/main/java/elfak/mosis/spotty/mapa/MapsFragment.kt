package elfak.mosis.spotty.mapa

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.opengl.Visibility
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import elfak.mosis.spotty.R
import elfak.mosis.spotty.account.AccountViewModel
import elfak.mosis.spotty.data.DownloadImageTask
import elfak.mosis.spotty.data.MarkerData
import elfak.mosis.spotty.data.Post
import elfak.mosis.spotty.databinding.FragmentMapsBinding

interface IMaps{
    fun addNewMarker(m:MarkerData)
}
class MapsFragment : Fragment(), IMaps {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MapsViewModel
    private lateinit var googleMap: GoogleMap
    private lateinit var locationProvider: FusedLocationProviderClient

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val LOCATION_PERMISSION_REQUEST_CODE=123
    private var initLocation: LatLng?=null
    private var clickedMarkerData: Post?=null
    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        this.googleMap = googleMap
        locationProvider = LocationServices.getFusedLocationProviderClient(requireActivity())
        fetchPoints()

        if(initLocation==null)
            centerToCurrentLoc()
        else
            centerToLocation(initLocation!!)
    }
    fun setInitLoc(ll:LatLng){
        initLocation=ll
    }
    private fun fetchPoints(){
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
            googleMap.isMyLocationEnabled=true
            locationProvider.getLastLocation().addOnSuccessListener { loc ->
                googleMap.clear()
                viewModel.fetchMapPoints(LatLng(loc.latitude,loc.longitude))
            }
        }
    }
    private fun centerToCurrentLoc(){
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
            googleMap.isMyLocationEnabled=true
            locationProvider.getLastLocation().addOnSuccessListener { loc ->
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(loc.latitude,loc.longitude))
                    .zoom(14.0f)
                    .build()
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
                googleMap.animateCamera(cameraUpdate, 2000, null)
            }
        }
    }

    private fun centerToLocation(ll:LatLng){
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
            googleMap.isMyLocationEnabled=true
            locationProvider.getLastLocation().addOnSuccessListener { loc ->
                val cameraPosition = CameraPosition.Builder()
                    .target(ll)
                    .zoom(14.0f)
                    .build()
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
                googleMap.animateCamera(cameraUpdate, 2000, null)
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        binding.button.setOnClickListener {
            fetchPoints()
        }
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                viewModel.changeData(
                    binding.editDistanceStart.text.toString(),
                    binding.editDistanceTo.text.toString()
                )
            }
        }
        binding.editDistanceStart.addTextChangedListener(afterTextChangedListener)
        binding.editDistanceTo.addTextChangedListener(afterTextChangedListener)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MapsViewModel::class.java)
        viewModel.mapCallback = this
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==LOCATION_PERMISSION_REQUEST_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                centerToCurrentLoc()
            }
            else{
                Toast.makeText(context,"Morate dozvoliti pristup lokaciji!",Toast.LENGTH_LONG)
            }
        }
    }

    override fun addNewMarker(m:MarkerData) {
        var marker =
            googleMap.addMarker(MarkerOptions().position(m.position).title(m.name))
        marker?.tag =m.postID
        googleMap.setOnMarkerClickListener { marker->
            FirebaseFirestore.getInstance().collection("posts").document(marker.tag as String).get().addOnSuccessListener {doc->
                val authorHashMap = doc["author"] as HashMap<*, *>
                clickedMarkerData= Post(
                    doc["authorID"] as String,
                    doc.id,
                    doc["postName"] as String,
                    authorHashMap["firstName"] as String + " " + authorHashMap["lastName"] as String,
                    doc["postDate"] as String,
                    doc["postDesc"] as String,
                    doc["postType"] as Long,
                    doc["geoPoint"] as GeoPoint,
                    doc["imgUrl"] as String,
                    doc["upvoteCount"] as Long,
                    doc["downvoteCount"] as Long
                )
                marker.showInfoWindow()
            }
            false
        }

        googleMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoContents(m: Marker): View? {
                if(clickedMarkerData==null) return null
                var v = layoutInflater.inflate(R.layout.home_post, null)
                var type: Int = R.drawable.ic_cafe
                if (clickedMarkerData!!.postType == 1L) type = R.drawable.ic_hotel
                else if (clickedMarkerData!!.postType == 2L) type = R.drawable.ic_restaurant
                else if (clickedMarkerData!!.postType >= 3L) type = R.drawable.ic_other
                v.findViewById<ImageView>(R.id.placeType)
                    .setImageDrawable(
                        requireContext().resources.getDrawable(
                            type,
                            requireContext().theme
                        )
                    )
                var dTask =
                    DownloadImageTask(v.findViewById<ImageView>(R.id.placeImage))
                dTask.execute(clickedMarkerData!!.imgUrl as String)
                v.findViewById<TextView>(R.id.postName).text = clickedMarkerData!!.postName
                v.findViewById<TextView>(R.id.postDesc).text = clickedMarkerData!!.postDesc
                v.findViewById<TextView>(R.id.postedBy).text = clickedMarkerData!!.postUser
                v.findViewById<TextView>(R.id.postDate).text = clickedMarkerData!!.postDate
                v.findViewById<ImageButton>(R.id.upvoteBtn).visibility=View.GONE
                v.findViewById<ImageButton>(R.id.downvoteBtn).visibility=View.GONE
                v.findViewById<TextView>(R.id.upvoteCount).visibility=View.GONE
                v.findViewById<TextView>(R.id.downvoteCount).visibility=View.GONE
                v.findViewById<ImageButton>(R.id.showOnMap).visibility=View.GONE
                return v
            }

            override fun getInfoWindow(p0: Marker): View? {
                return null
            }

        })
    }

}