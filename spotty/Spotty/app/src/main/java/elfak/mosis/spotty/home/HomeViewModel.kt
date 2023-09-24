package elfak.mosis.spotty.home

import android.media.Image
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import elfak.mosis.spotty.R
import elfak.mosis.spotty.data.Post
import elfak.mosis.spotty.register.RegisterFormState
import java.util.*
import kotlin.collections.HashMap

class HomeViewModel : ViewModel() {
    private val _posts = MutableLiveData<MutableList<Post>>()
    val posts: LiveData<MutableList<Post>> = _posts

    private val _userReactions = MutableLiveData<HashMap<String,Boolean>>()
    val userReactions: LiveData<HashMap<String,Boolean>> = _userReactions

    private val _downloaded = MutableLiveData<Boolean>()
    val downloaded: LiveData<Boolean> = _downloaded

    private val _newPosts = MutableLiveData<MutableList<Post>>()
    val newPosts: LiveData<MutableList<Post>> = _newPosts

    private val _hasNewPosts = MutableLiveData<Boolean>()
    val hasNewPosts: LiveData<Boolean> = _hasNewPosts

    private val _searchAuthorFirstName = MutableLiveData<String>()
    val searchAuthorFirstName: LiveData<String> = _searchAuthorFirstName

    private val _searchAuthorLastName = MutableLiveData<String>()
    val searchAuthorLastName: LiveData<String> = _searchAuthorLastName

    private val _searchTitle = MutableLiveData<String>()
    val searchTitle: LiveData<String> = _searchTitle

    lateinit var callback:IHome

    private var listOfIDs:MutableList<String> = mutableListOf()

    private val DOWNVOTE_PENALTY = 5
    private val UPVOTE_REWARD=5
    fun getPostsWithFilter(placeType:Long?): Query {
        var query:Query = FirebaseFirestore.getInstance().collection("posts")
        if(placeType!=null)
            query = query.where(Filter.equalTo("postType",placeType))
        return query
    }
    fun dataChanged(searchAuthorFirstName:String,searchAuthorLastName: String, searchTitle:String, ){
        _searchAuthorLastName.value = searchAuthorLastName
        _searchAuthorFirstName.value = searchAuthorFirstName
        _searchTitle.value = searchTitle
    }
    fun fetchPosts(placeType: Long?) {
        _downloaded.value = false
        if(_newPosts.value==null)
            _newPosts.value = mutableListOf()
        val task =  getPostsWithFilter(placeType)
        task.get().addOnSuccessListener { docs ->
            _posts.value = mutableListOf()
            for (doc in docs) {
                val authorHashMap = doc["author"] as HashMap<*, *>
                val firstName = authorHashMap["firstName"] as String
                val lastName = authorHashMap["lastName"] as String
                val postName = doc["postName"] as String
                if(searchAuthorFirstName.value!=null && !firstName.toLowerCase().contains(searchAuthorFirstName.value!!.toLowerCase()))
                    continue
                if(searchAuthorLastName.value!=null && !lastName.toLowerCase().contains(searchAuthorLastName.value!!.toLowerCase()))
                    continue
                if(searchTitle.value!=null && !postName.toLowerCase().contains(searchTitle.value!!.toLowerCase()))
                    continue
                _posts.value?.add(
                    Post(
                        doc["authorID"] as String,
                        doc.id,
                        postName,
                        firstName + " " + lastName,
                        doc["postDate"] as String,
                        doc["postDesc"] as String,
                        doc["postType"] as Long,
                        doc["geoPoint"] as GeoPoint,
                        doc["imgUrl"] as String,
                        doc["upvoteCount"] as Long,
                        doc["downvoteCount"] as Long
                    )
                )
                listOfIDs.add(doc.id)
            }
            FirebaseFirestore.getInstance().collection("users")
                .where(Filter.equalTo("id", FirebaseAuth.getInstance().uid))
                .get().addOnSuccessListener { docs ->
                    for (doc in docs) {
                        _userReactions.value = doc["reacted"] as HashMap<String, Boolean>
                    }
                    _downloaded.value = true
                }
            }

        task.addSnapshotListener{snap,e->
            if(e!=null){
                print(e.message)
                return@addSnapshotListener
            }
            for (dc in snap!!.documentChanges){
                val doc = dc.document.data
                when(dc.type){
                    DocumentChange.Type.MODIFIED-> callback.changeUpvoteDownvoteCount(dc.document.id,doc["upvoteCount"] as Long, doc["downvoteCount"] as Long)
                    DocumentChange.Type.ADDED ->{
                        if(dc.document.id in listOfIDs)
                            continue
                        _hasNewPosts.value=false
                        val authorHashMap = doc["author"] as HashMap<*, *>
                        _newPosts.value?.add(Post(
                            doc["authorID"] as String,
                            dc.document.id,
                            doc["postName"] as String,
                            authorHashMap["firstName"] as String + " " + authorHashMap["lastName"] as String,
                            doc["postDate"] as String,
                            doc["postDesc"] as String,
                            doc["postType"] as Long,
                            doc["geoPoint"] as GeoPoint,
                            doc["imgUrl"] as String,
                            doc["upvoteCount"] as Long,
                            doc["downvoteCount"] as Long
                        ))
                    }
                    else->{}
                }
            }
            _hasNewPosts.value = !newPosts.value?.size?.equals(0)!!

        }
    }
    fun removeNewPosts(){
        _newPosts.value?.clear()
        _hasNewPosts.value=false
    }
    private fun addUserReaction(){
        val updates = hashMapOf(
            "reacted" to _userReactions.value!!
            // Add other fields you want to update
        )
        FirebaseFirestore.getInstance().collection("users").where(Filter.equalTo("id",FirebaseAuth.getInstance().uid))
            .get().addOnSuccessListener { docs->
                for(doc in docs){
                    doc.reference.update(updates as Map<String, Any>)
                }
            }
    }
    private fun changePostCount(postID: String, upvotedChange:Long, downvoteChange:Long){
        FirebaseFirestore.getInstance().collection("posts").document(postID).get()
            .addOnSuccessListener { doc->
                val upvotes = doc.data?.get("upvoteCount") as Long
                val downvotes  = doc.data?.get("downvoteCount") as Long
                val updates = hashMapOf(
                    "upvoteCount" to (upvotes+upvotedChange),
                    "downvoteCount" to (downvotes+downvoteChange)
                )
                doc.reference.update(updates as Map<String, Any>)
                updatePosterPoints(doc.data?.get("authorID") as String,upvotedChange,downvoteChange)
            }
    }
    private fun updatePosterPoints(posterID:String, upvotedChange:Long, downvoteChange:Long){
        FirebaseFirestore.getInstance().collection("users").where(Filter.equalTo("id",posterID))
            .get()
            .addOnSuccessListener { docs->
                for (doc in docs){
                    val points = doc.data?.get("points") as Long
                    val updates = hashMapOf(
                        "points" to (points+upvotedChange*UPVOTE_REWARD-downvoteChange*DOWNVOTE_PENALTY)
                    )
                    doc.reference.update(updates as Map<String, Any>)
                }
            }
    }
    fun upvotePost(postID:String, callback:IHome, upBtn:ImageButton, downBtn:ImageButton, upvoteCnt:TextView,downvoteCnt:TextView){
        var userReactedToPost:Boolean = userReactions.value!!.contains(postID)
        if(userReactedToPost){
            //User vec reagovao
            var userUpvoted:Boolean = userReactions.value!![postID]!!
            //Uklanjamo upvote
            if(userUpvoted){
                callback.setNormalUpvote(upBtn,upvoteCnt)
                _userReactions.value?.remove(postID)
                changePostCount(postID,-1,0)
            }
            //Uklanjamo downvote, stavljamo upvote
            else{
                callback.setNormalDownvote(downBtn,downvoteCnt)
                callback.setUpvoted(upBtn,upvoteCnt)
                changePostCount(postID,1,-1)
                _userReactions.value?.set(postID, true)
            }
        }
        else{
            //User nije reagovao na post, sada se prvi put dodaje reakcija
            callback.setUpvoted(upBtn,upvoteCnt)
            changePostCount(postID,1,0)
            _userReactions.value?.set(postID, true)
        }
        addUserReaction()
    }
    fun downvotePost(postID:String, callback:IHome, upBtn:ImageButton, downBtn:ImageButton,upvoteCnt:TextView,downvoteCnt:TextView){
        var userReactedToPost:Boolean = userReactions.value!!.contains(postID)
        if(userReactedToPost){
            //User vec reagovao
            var userUpvoted:Boolean = userReactions.value!![postID]!!
            //Uklanjamo upvote, stavljamo downvote
            if(userUpvoted){
                _userReactions.value?.set(postID, false)
                callback.setNormalUpvote(upBtn,upvoteCnt)
                callback.setDownvoted(downBtn,downvoteCnt)
                changePostCount(postID,-1,1)
            }
            //Uklanjamo downvote
            else{
                callback.setNormalDownvote(downBtn,downvoteCnt)
                changePostCount(postID,0,-1)
                _userReactions.value?.remove(postID)
            }
        }
        else{
            //User nije reagovao na post, sada se prvi put dodaje reakcija
            callback.setDownvoted(downBtn,downvoteCnt)
            changePostCount(postID,0,1)
            _userReactions.value?.set(postID, false)
        }
        addUserReaction()
    }
}