package com.example.trelloclone.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.example.trelloclone.R
import com.example.trelloclone.firebase.FireStoreClass
import com.example.trelloclone.models.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // for full screen view
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN
//        )

        setUpActionBar()
    }

    fun userRegisteredSuccess(){
        Toast.makeText(this,"you have "+"successfully registered",Toast.LENGTH_LONG)
            .show()

        hideProgressDialog()

        FirebaseAuth.getInstance().signOut()
        finish()
    }

    // for back button on sign up activity page
    private fun setUpActionBar(){
        val toolbar_sign_up_activity:Toolbar = findViewById(R.id.toolbar_sign_up_activity)
        setSupportActionBar(toolbar_sign_up_activity)

        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
        }

        toolbar_sign_up_activity.setNavigationOnClickListener{onBackPressed()}

        val btn_sign_up : Button =findViewById(R.id.btn_sign_up)
        btn_sign_up.setOnClickListener{
            registerUser()
        }
    }

    private fun registerUser(){
        val et_name:TextView = findViewById(R.id.et_name)
        val et_email:TextView = findViewById(R.id.et_email)
        val et_password:TextView = findViewById(R.id.et_password)

        val name: String = et_name.text.toString().trim{it <= ' '}
        val email: String = et_email.text.toString().trim{it <= ' '}
        val password: String = et_password.text.toString().trim{it <= ' '}

        if(validateForm(name,email,password)){
            showProgressDialog(resources.getString(R.string.please_wait))

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser = task.result!!.user!!
                    val registeredEmail = firebaseUser.email!!
                    val user = User(firebaseUser.uid,name,registeredEmail)

                    FireStoreClass().registerUser(this,user)

                } else {
                    Toast.makeText(
                        this, "Registration failed", Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }

    }

    private fun validateForm(name: String,email:String,password:String) : Boolean{
        return when{
            TextUtils.isEmpty(name)->{
                showErrorSnackBar("Please enter a name")
                false
            }
            TextUtils.isEmpty(email)->{
                showErrorSnackBar("Please enter a name")
                false
            }
            TextUtils.isEmpty(password)->{
                showErrorSnackBar("Please enter a name")
                false
            }
            else->{
                return true
            }

        }
    }
}