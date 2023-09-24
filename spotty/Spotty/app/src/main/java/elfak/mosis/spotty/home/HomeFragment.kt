package elfak.mosis.spotty.home

import android.media.Image
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.maps.model.LatLng
import elfak.mosis.spotty.IMainActivity
import elfak.mosis.spotty.R
import elfak.mosis.spotty.data.DownloadImageTask
import elfak.mosis.spotty.databinding.FragmentAccountBinding
import elfak.mosis.spotty.databinding.FragmentHomeBinding
import java.util.*
import kotlin.collections.HashMap

interface IHome{
    fun setUpvoted(btn:ImageButton, text: TextView)
    fun setDownvoted(btn:ImageButton, text: TextView)
    fun setNormalUpvote(btn:ImageButton, text: TextView)
    fun setNormalDownvote(btn:ImageButton, text: TextView)
    fun changeUpvoteDownvoteCount(id:String, upvotes:Long,downvotes:Long)
}
class HomeFragment() : Fragment(),IHome {

    private var callback:IMainActivity?=null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private var placeType:Long?=null

    private lateinit var viewIdHashMap: HashMap<String,View>
    var filterVisible = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    fun setCallback(_callback:IMainActivity){
        callback=_callback
    }

    private fun addNewPosts(){
        var linearLayout = binding.linearPosts
        for (post in viewModel.newPosts.value!!){
            val customView = layoutInflater.inflate(R.layout.home_post, linearLayout, false)
            viewIdHashMap[post.postID] = customView
            var type:Int = R.drawable.ic_cafe
            if(post.postType==1L) type = R.drawable.ic_hotel
            else if (post.postType==2L) type= R.drawable.ic_restaurant
            else if(post.postType>=3L) type = R.drawable.ic_other
            customView.findViewById<ImageView>(R.id.placeType).setImageDrawable(requireContext().resources.getDrawable(type,requireContext().theme))
            var dTask = DownloadImageTask(customView.findViewById<ImageView>(R.id.placeImage))
            dTask.execute(post.imgUrl)
            customView.findViewById<TextView>(R.id.postName).text = post.postName
            customView.findViewById<TextView>(R.id.postDesc).text = post.postDesc
            customView.findViewById<TextView>(R.id.postedBy).text = post.postUser
            customView.findViewById<TextView>(R.id.postDate).text = post.postDate
            customView.findViewById<TextView>(R.id.upvoteCount).text = post.upvoteCount.toString()
            customView.findViewById<TextView>(R.id.downvoteCount).text = post.downvoteCount.toString()
            var upvoteBtn = customView.findViewById<ImageButton>(R.id.upvoteBtn)
            var downvoteBtn = customView.findViewById<ImageButton>(R.id.downvoteBtn)
            var upvoteCnt = customView.findViewById<TextView>(R.id.upvoteCount)
            var downvoteCnt = customView.findViewById<TextView>(R.id.downvoteCount)

            customView.findViewById<ImageButton>(R.id.showOnMap).setOnClickListener{
                callback?.changeToMapAndCenter(
                    com.google.android.gms.maps.model.LatLng(
                        post.geoPoint.latitude,
                        post.geoPoint.longitude
                    )
                )
            }
            upvoteBtn.setOnClickListener{
                viewModel.upvotePost(post.postID,this,upvoteBtn,downvoteBtn,upvoteCnt,downvoteCnt)
            }
            downvoteBtn.setOnClickListener{
                viewModel.downvotePost(post.postID,this,upvoteBtn,downvoteBtn,upvoteCnt,downvoteCnt)
            }
            linearLayout.addView(customView)
        }
        viewModel.removeNewPosts()

    }
    private fun populateLinearLayout(){
        var linearLayout = binding.linearPosts
        linearLayout.removeAllViews()
        for (post in viewModel.posts.value!!){
            val customView = layoutInflater.inflate(R.layout.home_post, linearLayout, false)
            viewIdHashMap[post.postID] = customView
            var type:Int = R.drawable.ic_cafe
            if(post.postType==1L) type = R.drawable.ic_hotel
            else if (post.postType==2L) type= R.drawable.ic_restaurant
            else if(post.postType>=3L) type = R.drawable.ic_other
            customView.findViewById<ImageView>(R.id.placeType).setImageDrawable(requireContext().resources.getDrawable(type,requireContext().theme))
            var dTask = DownloadImageTask(customView.findViewById<ImageView>(R.id.placeImage))
            dTask.execute(post.imgUrl)
            customView.findViewById<TextView>(R.id.postName).text = post.postName
            customView.findViewById<TextView>(R.id.postDesc).text = post.postDesc
            customView.findViewById<TextView>(R.id.postedBy).text = post.postUser
            customView.findViewById<TextView>(R.id.postDate).text = post.postDate
            customView.findViewById<TextView>(R.id.upvoteCount).text = post.upvoteCount.toString()
            customView.findViewById<TextView>(R.id.downvoteCount).text = post.downvoteCount.toString()
            var upvoteBtn = customView.findViewById<ImageButton>(R.id.upvoteBtn)
            var downvoteBtn = customView.findViewById<ImageButton>(R.id.downvoteBtn)
            var upvoteCnt = customView.findViewById<TextView>(R.id.upvoteCount)
            var downvoteCnt = customView.findViewById<TextView>(R.id.downvoteCount)
            if(viewModel.userReactions.value!!.contains(post.postID)){
                if(viewModel.userReactions.value!![post.postID]!!){
                    upvoteBtn.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_upvote_clicked,requireContext().theme))
                }
                else{
                    downvoteBtn.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_downvote_clicked,requireContext().theme))
                }
            }
            customView.findViewById<ImageButton>(R.id.showOnMap).setOnClickListener{
                callback?.changeToMapAndCenter(
                    com.google.android.gms.maps.model.LatLng(
                        post.geoPoint.latitude,
                        post.geoPoint.longitude
                    )
                )
            }
            upvoteBtn.setOnClickListener{
                viewModel.upvotePost(post.postID,this,upvoteBtn,downvoteBtn,upvoteCnt,downvoteCnt)
            }
            downvoteBtn.setOnClickListener{
                viewModel.downvotePost(post.postID,this,upvoteBtn,downvoteBtn,upvoteCnt,downvoteCnt)
            }
            linearLayout.addView(customView)
        }
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewIdHashMap = hashMapOf()
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        viewModel.callback = this
        // TODO: Use the ViewModel
        viewModel.fetchPosts(placeType)
        viewModel.downloaded.observe(viewLifecycleOwner, Observer { downloaded->
            if(downloaded)
                populateLinearLayout()
        })
        binding.filterBtn.setOnClickListener {
            filterVisible = !filterVisible

            if(filterVisible) {
                binding.filterLayout.visibility = View.VISIBLE
                binding.filterBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_up),
                    null
                )
            }
            else {
                binding.filterLayout.visibility = View.GONE
                binding.filterBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_down),
                    null
                )
            }
        }
        binding.btnSearch.setOnClickListener {
            viewModel.fetchPosts(placeType)
        }
        viewModel.hasNewPosts.observe(viewLifecycleOwner, Observer { hasNewPosts->
            if(hasNewPosts)
                addNewPosts()
        })
        binding.allRB.setOnClickListener{
            placeType=null
            viewModel.fetchPosts(placeType)
        }
        binding.caffeRB.setOnClickListener{
            placeType=0
            viewModel.fetchPosts(placeType)
        }
        binding.hotelRB.setOnClickListener{
            placeType=1
            viewModel.fetchPosts(placeType)
        }
        binding.restRB.setOnClickListener{
            placeType=2
            viewModel.fetchPosts(placeType)
        }
        binding.otherRB.setOnClickListener{
            placeType=3
            viewModel.fetchPosts(placeType)
        }
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                viewModel.dataChanged(
                    binding.filterAuthorFirstnameText.text.toString(),
                    binding.filterAuthorLastnameText.text.toString(),
                    binding.filterText.text.toString()
                )
            }
        }
        binding.filterAuthorFirstnameText.addTextChangedListener(afterTextChangedListener)
        binding.filterAuthorLastnameText.addTextChangedListener(afterTextChangedListener)
        binding.filterText.addTextChangedListener(afterTextChangedListener)

    }

    override fun setUpvoted(btn: ImageButton,text: TextView) {
        text.text = (text.text.toString().toLong()+1).toString()
        btn.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_upvote_clicked,requireContext().theme))
    }

    override fun setDownvoted(btn: ImageButton,text: TextView) {
        text.text = (text.text.toString().toLong()+1).toString()
        btn.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_downvote_clicked,requireContext().theme))
    }

    override fun setNormalUpvote(btn: ImageButton,text: TextView) {
        text.text = (text.text.toString().toLong()-1).toString()
        btn.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_upvote,requireContext().theme))
    }

    override fun setNormalDownvote(btn: ImageButton,text: TextView) {
        text.text = (text.text.toString().toLong()-1).toString()
        btn.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_downvote,requireContext().theme))
    }

    override fun changeUpvoteDownvoteCount(id: String, upvotes: Long, downvotes: Long) {
        viewIdHashMap[id]?.findViewById<TextView>(R.id.upvoteCount)?.text = upvotes.toString()
        viewIdHashMap[id]?.findViewById<TextView>(R.id.downvoteCount)?.text = downvotes.toString()
    }

}