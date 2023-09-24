package elfak.mosis.spotty.mapa

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import elfak.mosis.spotty.data.MarkerData

class MapsViewModel : ViewModel() {
    private var _points = MutableLiveData<MutableList<MarkerData>>()
    val points: LiveData<MutableList<MarkerData>>
        get() = _points

    private var _minDistance = MutableLiveData<Float?>()
    val minDistance: LiveData<Float?>
        get() = _minDistance

    private var _maxDistance = MutableLiveData<Float?>()
    val maxDistance: LiveData<Float?>
        get() = _maxDistance

    fun changeData(min:String,max:String){
        try{
            _minDistance.value = min.toFloat()
        }
        catch (e:java.lang.Exception){
            _minDistance.value = null
        }
        try{
            _maxDistance.value = max.toFloat()
        }
        catch (e:java.lang.Exception){
            _maxDistance.value = null
        }
    }
    lateinit var mapCallback:IMaps
    fun fetchMapPoints(currPos:LatLng){
        _points.value = mutableListOf()
        FirebaseFirestore.getInstance().collection("posts").addSnapshotListener { qS,e->
            if(e!=null){
                print(e.message)
                return@addSnapshotListener
            }
            for(dc in qS!!.documentChanges){
                val doc = dc.document
                when(dc.type) {
                    DocumentChange.Type.ADDED-> {
                        val geoP = doc["geoPoint"] as GeoPoint
                        val marker = MarkerData(
                            LatLng(geoP.latitude, geoP.longitude),
                            doc["postName"] as String,
                            doc.id
                        )
                        _points.value?.add(marker)
                        val res = FloatArray(1)
                        Location.distanceBetween(marker.position.latitude,marker.position.longitude,currPos.latitude,currPos.longitude,res)
                        res[0] = res[0]/1000
                        var minDistanceCorrect = (minDistance.value!=null && minDistance.value!!<=res[0]) || minDistance.value==null
                        var maxDistanceCorrect = (maxDistance.value!=null && maxDistance.value!!>=res[0]) || maxDistance.value==null
                        if(minDistanceCorrect && maxDistanceCorrect)
                            mapCallback.addNewMarker(marker)
                    }
                    else->{}
                }
            }
        }
    }

}