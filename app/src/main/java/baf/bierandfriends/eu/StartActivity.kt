package baf.bierandfriends.eu

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import baf.bierandfriends.eu.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: Splashscreen-Timer
        binding.root.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }
}
