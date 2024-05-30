package com.example.wificonnector

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class CropActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)
        
        val imagePath = intent.getStringExtra("image_path")

        if (imagePath != null) {
            Log.d(TAG, "onCreate: $imagePath")
        }
    }
}