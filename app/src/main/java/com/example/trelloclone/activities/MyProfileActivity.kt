package com.example.trelloclone.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.trelloclone.R
import com.example.trelloclone.firebase.FireStoreClass
import com.example.trelloclone.models.User
import com.example.trelloclone.utils.Constants
import com.example.trelloclone.utils.Constants.PICK_IMAGE_REQUEST_CODE
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? =null
    private lateinit var mUserDetails : User
    private var mProfileImageURL : String=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setUpActionBar()

        FireStoreClass().loadUserData(this)
        val iv_profile_user_image : CircleImageView = findViewById(R.id.iv_profile_user_image)

        iv_profile_user_image.setOnClickListener{
//            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
//            == PackageManager.PERMISSION_GRANTED){
                // TODO Show Image Chooser
                Constants.showImageChooser(this)
//            }else{
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                    Constants.READ_STORAGE_PERMISSION_CODE
//                )
//            }
        }

        val btn_update : Button = findViewById(R.id.btn_update)
        btn_update.setOnClickListener{
            if(mSelectedImageFileUri!=null){
                uploadUserImage()
            }
            else{
                showProgressDialog(resources.getString(R.string.please_wait))

                updateUserProfileData()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.READ_STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // TODO Show Image Chooser
                showImageChooser()
            }
        }else{
            Toast.makeText(
                this,"Storage permission denied. You can allow it from settings",Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showImageChooser(){
        var galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(galleryIntent, Constants.PICK_IMAGE_REQUEST_CODE)
    }

    private fun getFileExtension(uri: Uri):String?{
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(contentResolver.getType(uri!!))
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST_CODE && data?.data!=null){
            mSelectedImageFileUri = data.data

            try {
                val iv_user_image: CircleImageView = findViewById(R.id.iv_profile_user_image)
                Glide
                    .with(this@MyProfileActivity)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(iv_user_image)
            }
            catch (e:IOException){
                e.printStackTrace()
            }
        }
    }

    private fun setUpActionBar(){
        val toolbar_main_activity : Toolbar = findViewById(R.id.toolbar_my_profile_activity)
        setSupportActionBar(toolbar_main_activity)

        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)

            actionBar.title = resources.getString(R.string.my_profile_title)

            val toolbar_my_profile_activity : Toolbar = findViewById(R.id.toolbar_my_profile_activity)
            toolbar_my_profile_activity.setNavigationOnClickListener{onBackPressed()}
        }
    }

    fun setUserDataInUI(user: User){

        mUserDetails=user

        val iv_user_image : CircleImageView = findViewById(R.id.iv_profile_user_image)
        Glide
            .with(this@MyProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(iv_user_image)

        val et_name : AppCompatEditText = findViewById(R.id.et_name)
        val et_email : AppCompatEditText = findViewById(R.id.et_email)

        et_name.setText(user.name)
        et_email.setText(user.email)
        if(user.mobile!=0L){
            val et_mobile : AppCompatEditText = findViewById(R.id.et_mobile)
            et_mobile.setText(user.mobile.toString())
        }
    }

    private fun updateUserProfileData(){
        val userHashMap = HashMap<String,Any>()

        var anyChangesMade = false

        if(mProfileImageURL.isNotEmpty() && mProfileImageURL!=mUserDetails.image){
            // userHashMap["image"]
            userHashMap[Constants.IMAGE] = mProfileImageURL
            anyChangesMade=true;
        }

        val et_name : EditText = findViewById(R.id.et_name)
        if(et_name.text.toString()!=mUserDetails.name){
            userHashMap[Constants.NAME] = et_name.text.toString()
            anyChangesMade=true
        }

        val et_mobile : EditText = findViewById(R.id.et_mobile)
        if(et_mobile.text.toString()!=mUserDetails.mobile.toString()){
            userHashMap[Constants.MOBILE] = et_mobile.text.toString().toLong()
            anyChangesMade=true
        }

        if(anyChangesMade) {
            FireStoreClass().updateUserProfileData(this, userHashMap)
        }
    }

    private fun uploadUserImage(){
        showProgressDialog(resources.getString(R.string.please_wait))

        if(mSelectedImageFileUri!=null){
            val sRef : StorageReference =
                FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE"+System.currentTimeMillis()
            +"."+ getFileExtension(mSelectedImageFileUri!!))

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot->
                Log.i(
                    "Firebase Image URL",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri->
                    Log.i("Downloadable Image URL",uri.toString())
                    mProfileImageURL = uri.toString()

                    hideProgressDialog()
                    // TODO UpdateUserProfileData
                    updateUserProfileData()

                }
            }.addOnFailureListener{
                exception->
                Toast.makeText(
                    this@MyProfileActivity,
                    exception.message,
                    Toast.LENGTH_LONG
                ).show()

                hideProgressDialog()
            }
        }
    }

    fun profileUpdateSuccess(){
        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()

    }
}