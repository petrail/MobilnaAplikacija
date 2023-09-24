package elfak.mosis.spotty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth


class LoginRegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        actionBar?.hide()
        var w = this.window
        w.statusBarColor = this.resources.getColor(R.color.background)
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_register)
        if(FirebaseAuth.getInstance().currentUser!=null)
            skipLogin()

    }

    fun skipLogin() {
        val intent = Intent(this@LoginRegisterActivity, MainActivity::class.java)
        startActivity(intent)
    }
}