package elfak.mosis.spotty.rank

import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.spotty.R
import elfak.mosis.spotty.data.DownloadImageTask
import elfak.mosis.spotty.data.Rank

class RankViewModel : ViewModel() {
    private val _otherRanks = MutableLiveData<MutableList<Rank>>()
    val otherRank: LiveData<MutableList<Rank>> = _otherRanks

    private val _myRank = MutableLiveData<Rank>()
    val myRank: LiveData<Rank> = _myRank

    fun fetchData(bestLimit:Long, callback:IRank){
        Firebase.firestore.collection("users").where(Filter.equalTo("id", FirebaseAuth.getInstance().uid))
            .addSnapshotListener { qS,e->
                if(e!=null){
                    print(e.message)
                    return@addSnapshotListener
                }
                for(dc in qS!!.documentChanges){
                    val doc = dc.document
                    when(dc.type) {
                        DocumentChange.Type.ADDED-> {
                            _myRank.value = Rank(
                                doc["firstName"] as String + " " + doc["lastName"] as String,
                                doc["imageUrl"] as String,
                                doc["points"] as Long
                            )
                            callback.addLeaderboardItem(myRank.value!!, true)
                        }
                        else->{}
                    }
                }
            }
        Firebase.firestore.collection("users").orderBy("points", Query.Direction.DESCENDING).limit(bestLimit)
            .addSnapshotListener {  qS,e->
                if(e!=null){
                    print(e.message)
                    return@addSnapshotListener
                }
                _otherRanks.value = mutableListOf()
                callback.clearLeaderboard()
                for (doc in qS!!.documents){
                    val R = Rank(
                        doc["firstName"] as String + " " + doc["lastName"] as String,
                        doc["imageUrl"] as String,
                        doc["points"] as Long
                    )
                    _otherRanks.value!!.add(R)

                    callback.addLeaderboardItem(R, false)
                    }
                }
            }
    }
