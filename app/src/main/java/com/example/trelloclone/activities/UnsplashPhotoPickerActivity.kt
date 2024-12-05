package com.example.trelloclone.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.trelloclone.R
import com.example.trelloclone.adapters.PhotoAdapter
import com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto
import com.unsplash.pickerandroid.photopicker.presentation.UnsplashPickerActivity

class UnsplashPhotoPickerActivity : AppCompatActivity() {

    private lateinit var mAdapter: PhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_unsplash_photo_picker)

        val main_recycler_view : RecyclerView = findViewById(R.id.main_recycler_view)
        val main_pick_button : Button = findViewById(R.id.main_pick_button)
        val main_single_radio_button : RadioButton = findViewById(R.id.main_single_radio_button)

        main_recycler_view.setHasFixedSize(true)
        main_recycler_view.itemAnimator = null
        main_recycler_view.layoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        mAdapter = PhotoAdapter(this)
        main_recycler_view.adapter = mAdapter
        // on the pick button click, we start the library picker activity
        // we are expecting a result from it so we start it for result
        main_pick_button.setOnClickListener {
            startActivityForResult(
                UnsplashPickerActivity.getStartingIntent(
                    this,
                    !main_single_radio_button.isChecked
                ), REQUEST_CODE
            )
        }
    }

    // here we are receiving the result from the picker activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            // getting the photos
            val photos: ArrayList<UnsplashPhoto>? = data?.getParcelableArrayListExtra(
                UnsplashPickerActivity.EXTRA_PHOTOS)
            // showing the preview
            mAdapter.setListOfPhotos(photos)
            // telling the user how many have been selected
            Toast.makeText(this, "number of selected photos: " + photos?.size, Toast.LENGTH_SHORT).show()

            // Pass the URL of the first selected photo to BackgroundImageActivity
            photos?.let {
                if (it.isNotEmpty()) {

                    val intent = Intent(this, TaskListActivity::class.java)
                    intent.putExtra("BACKGROUND_IMAGE", it[0].urls.full) // Pass the full image URL
                    //startActivity(intent)

                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    companion object {
        // dummy request code to identify the request
        private const val REQUEST_CODE = 123
    }

}