package elfak.mosis.spotty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.spotty.account.AccountFragment
import elfak.mosis.spotty.addPost.AddPostFragment
import elfak.mosis.spotty.data.DownloadImageTask
import elfak.mosis.spotty.home.HomeFragment
import elfak.mosis.spotty.mapa.MapsFragment
import elfak.mosis.spotty.rank.RankFragment

interface IMainActivity{
    fun logout()
    fun changeToMapAndCenter(ll: LatLng)
    fun backToMain()
}
class MainActivity : AppCompatActivity(), IMainActivity {
    override fun onCreate(savedInstanceState: Bundle?) {
        actionBar?.hide()
        var w = this.window
        w.statusBarColor = this.resources.getColor(R.color.background)
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var h = HomeFragment()
        h.setCallback(this)
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId= R.id.button_home

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
            .replace(R.id.fragmentMainContainer, h)
            .commit()

        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item->
            when(item.itemId){
                R.id.button_home-> {
                    var h = HomeFragment()
                    h.setCallback(this)
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                        .replace(R.id.fragmentMainContainer, h)
                        .commit()
                    true
                }
                R.id.button_add -> {
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                        .replace(R.id.fragmentMainContainer, AddPostFragment(this))
                        .commit()
                    true
                }
                R.id.button_map -> {
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                        .replace(R.id.fragmentMainContainer, MapsFragment())
                        .commit()
                    true
                }
                R.id.button_rank -> {
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                        .replace(R.id.fragmentMainContainer, RankFragment())
                        .commit()
                    true
                }
                R.id.button_account -> {
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                        .replace(R.id.fragmentMainContainer, AccountFragment(this))
                        .commit()
                    true
                }
                else -> {
                    false
                }
            }

        }
        findViewById<ImageButton>(R.id.logoutBtn).setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this@MainActivity, LoginRegisterActivity::class.java)
            startActivity(intent)
        }


        Firebase.firestore.collection("users").where(Filter.equalTo("id", FirebaseAuth.getInstance().uid))
            .get().addOnSuccessListener { docs->
                for(doc in docs){
                    findViewById<TextView>(R.id.name).text =
                        doc.data["firstName"] as String +
                                " "+
                                doc.data["lastName"] as String
                    var dTask = DownloadImageTask(findViewById<ImageView>(R.id.acc_image))
                    dTask.execute(doc.data["imageUrl"] as String)
                }
            }
    }

    override fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this@MainActivity, LoginRegisterActivity::class.java)
        startActivity(intent)
    }

    override fun changeToMapAndCenter(ll:LatLng) {
        var m = MapsFragment()
        m.setInitLoc(ll)
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId= R.id.button_map
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
            .replace(R.id.fragmentMainContainer, m)
            .commit()
    }

    override fun backToMain() {
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId= R.id.button_home
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
            .replace(R.id.fragmentMainContainer, HomeFragment())
            .commit()
    }
}