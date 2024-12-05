package com.example.trelloclone.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.trelloclone.R
import com.example.trelloclone.firebase.FireStoreClass
import com.example.trelloclone.models.Board
import com.example.trelloclone.utils.Constants
import com.example.trelloclone.utils.Constants.showImageChooser
import com.google.common.io.Files.getFileExtension
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException

class CreateBoardActivity : BaseActivity() {

    private var mSelectedImageFileUri : Uri? = null

    private lateinit var mUserName : String

    private var mBoardImageURL : String = ""

//    private var mBoardBackgroundImageURL : String = ""
//    private var mSelectedBackgroundImageUri: Uri? = null

    private lateinit var imagePreview: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_board)

        setUpActionBar()

        if(intent.hasExtra(Constants.NAME)){
            mUserName = intent.getStringExtra(Constants.NAME).toString()
        }

        val iv_board_image: CircleImageView = findViewById(R.id.iv_board_image)

        iv_board_image.setOnClickListener{

                // TODO Show Image Chooser
                Constants.showImageChooser(this)
        }

        val btn_create : Button = findViewById(R.id.btn_create)

        btn_create.setOnClickListener{
            if(mSelectedImageFileUri!=null){
                uploadBoardImage()
            }
            else{
                showProgressDialog(resources.getString(R.string.please_wait))
                createBoard()
            }
        }

    }


    private fun createBoard(){
        val assignedUsersArrayList: ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserID())

        val et_board_name : AppCompatEditText = findViewById(R.id.et_board_name)
        val board_name : String = et_board_name.text.toString()

        if(board_name.isNotEmpty()) {
            //val ll : LinearLayout = findViewById(R.id.task_list_board_bg)

            // Set default background image from drawable
            val defaultBackgroundUri = Uri.parse("android.resource://${packageName}/drawable/ic_bg").toString()
            var board = Board(
                et_board_name.text.toString(),
                mBoardImageURL,
                defaultBackgroundUri,
                mUserName,
                assignedUsersArrayList
            )

            FireStoreClass().createBoard(this, board)
        }
        else{
            Toast.makeText(this,"Please enter board name",Toast.LENGTH_SHORT).show()
            hideProgressDialog()
        }

    }

    private fun uploadBoardImage(){
        showProgressDialog(resources.getString(R.string.please_wait))

        if(mSelectedImageFileUri!=null) {
            val sRef: StorageReference =
                FirebaseStorage.getInstance().reference.child(
                    "BOARD_IMAGE" + System.currentTimeMillis()
                            + "." + Constants.getFileExtension(this, mSelectedImageFileUri!!)
                )

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                    taskSnapshot ->
                Log.i(
                    "Board Image URL",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    Log.i("Downloadable Image URL", uri.toString())
                    mBoardImageURL = uri.toString()

                    hideProgressDialog()

                    // TODO UpdateUserProfileData
                    createBoard()

                }

            }.addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    exception.message,
                    Toast.LENGTH_LONG
                ).show()

                hideProgressDialog()
            }
        }


    }


    fun boardCreatedSuccessfully(){
        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }


    private fun setUpActionBar(){
        val toolbar_create_board_activity : Toolbar = findViewById(R.id.toolbar_create_board_activity)
        setSupportActionBar(toolbar_create_board_activity)

        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)

            actionBar.title =resources.getString(R.string.create_board_title)

            val toolbar_create_board_activity : Toolbar = findViewById(R.id.toolbar_create_board_activity)
            toolbar_create_board_activity.setNavigationOnClickListener{onBackPressed()}

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
                Constants.showImageChooser(this)
            }
        }else{
            Toast.makeText(
                this,"Oops, you just denied the permission for storage. You can allow it from settings",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == Constants.PICK_IMAGE_REQUEST_CODE && data!!.data!=null){
            mSelectedImageFileUri = data.data

            try {
                val iv_board_image: CircleImageView = findViewById(R.id.iv_board_image)
                Glide
                    .with(this)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_board_place_holder)
                    .into(iv_board_image)
            }
            catch (e: IOException){
                e.printStackTrace()
            }
        }


    }



    companion object {
        const val PICK_IMAGE_REQUEST = 1
    }
}