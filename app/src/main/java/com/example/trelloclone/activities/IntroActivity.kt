package com.example.trelloclone.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import com.example.trelloclone.R

class IntroActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val btn_sign_up : Button = findViewById(R.id.intro_btn_sign_up)
        btn_sign_up.setOnClickListener{
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        val btn_sign_in : Button = findViewById(R.id.intro_btn_sign_in)
        btn_sign_in.setOnClickListener{
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}