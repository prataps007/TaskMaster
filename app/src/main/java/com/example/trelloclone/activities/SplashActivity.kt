package com.example.trelloclone.activities

import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.TextView
import com.example.trelloclone.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val typeFace: Typeface = Typeface.createFromAsset(assets,"carbon_bl.ttf")
        val tv_app_name : TextView = findViewById(R.id.tv_app_name)

        tv_app_name.typeface = typeFace

        Handler().postDelayed({
            // Start the main activity after the specified duration
            val mainIntent = Intent(this@SplashActivity, IntroActivity::class.java)
            startActivity(mainIntent)
            finish()
        }, 2500.toLong())

    }
}