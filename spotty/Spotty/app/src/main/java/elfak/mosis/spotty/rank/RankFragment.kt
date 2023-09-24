package elfak.mosis.spotty.rank

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.spotty.R
import elfak.mosis.spotty.data.DownloadImageTask
import elfak.mosis.spotty.data.Rank
import elfak.mosis.spotty.databinding.FragmentAddPostBinding
import elfak.mosis.spotty.databinding.FragmentRankBinding
import java.io.InputStream
import java.net.URL

interface IRank{
    fun addLeaderboardItem(R: Rank, isMyRank: Boolean)
    fun clearLeaderboard()
}
class RankFragment : Fragment(), IRank {

    private lateinit var viewModel: RankViewModel
    private val bestLimit:Long=10

    private var _binding: FragmentRankBinding? = null
    private val binding get() = _binding!!

    private lateinit var rInflater: LayoutInflater

    private var myRankID: Int=0
    private var leaderboardItemID: Int = 0
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rInflater = inflater
        _binding = FragmentRankBinding.inflate(inflater, container, false)
        myRankID = R.layout.myrank
        leaderboardItemID = R.layout.leaderboard_item
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RankViewModel::class.java)
        // TODO: Use the ViewModel
        viewModel.fetchData(bestLimit,this)
    }

    override fun addLeaderboardItem(R: Rank, isMyRank: Boolean) {
        if(isMyRank) binding.rankList.removeAllViews()
        val customView = if(isMyRank) rInflater.inflate(myRankID, binding.rankList, false) else
                                      rInflater.inflate(leaderboardItemID, binding.rankList, false)
        customView.findViewById<TextView>(elfak.mosis.spotty.R.id.accountPoints).text = R.accountPoints.toString()
        customView.findViewById<TextView>(elfak.mosis.spotty.R.id.accountName).text = R.accountName
        var dTask = DownloadImageTask(customView.findViewById<ImageView>(elfak.mosis.spotty.R.id.accountImage))
        dTask.execute(R.accountImgUrl)
        if(isMyRank){
            binding.myRank.addView(customView)
        }
        else{
            binding.rankList.addView(customView)
        }
    }

    override fun clearLeaderboard() {
        binding.rankList.removeAllViews()
    }

}