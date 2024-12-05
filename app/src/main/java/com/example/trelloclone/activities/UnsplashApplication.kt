package com.example.trelloclone.activities

import android.app.Application
import com.unsplash.pickerandroid.photopicker.UnsplashPhotoPicker

class UnsplashApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the Unsplash Photo Picker
        UnsplashPhotoPicker.init(
            this,
            "IbnuRP7timEy5muyJngrDQ03Zop_j9Ck4XuSjUe0RyU",  // Replace with your Unsplash Access Key
            "_7ySr2h0I3KYE-PS3yt-vCLh965YaHnIN12zuicfbPs"   // Replace with your Unsplash Secret Key
        )
    }
}