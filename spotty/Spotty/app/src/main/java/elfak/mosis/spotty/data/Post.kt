package elfak.mosis.spotty.data

import com.google.firebase.firestore.GeoPoint

data class Post (
        val postUserID:String,
        val postID:String,
        val postName:String,
        val postUser:String,
        val postDate: String,
        val postDesc:String,
        val postType:Long,
        val geoPoint:GeoPoint,
        val imgUrl: String,
        val upvoteCount:Long,
        val downvoteCount:Long,
        )