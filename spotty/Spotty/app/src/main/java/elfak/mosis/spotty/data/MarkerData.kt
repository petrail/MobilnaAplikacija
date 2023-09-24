package elfak.mosis.spotty.data

import com.google.android.gms.maps.model.LatLng

data class MarkerData(
    val position: LatLng,
    val name: String,
    val postID:String,
)
