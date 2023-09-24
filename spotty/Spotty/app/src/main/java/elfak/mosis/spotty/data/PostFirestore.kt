package elfak.mosis.spotty.data

import com.google.firebase.firestore.GeoPoint

data class PostFirestore(
    val authorID:String,
    val author:HashMap<String,String>,
    val downvoteCount: Long,
    val geoPoint: GeoPoint,
    val imgUrl: String,
    val postName:String,
    val postDate: String,
    val postDesc:String,
    val postType:Long,
    val upvoteCount:Long, )
