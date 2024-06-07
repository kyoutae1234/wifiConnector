package com.example.wificonnector

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ConnectActivity : AppCompatActivity() {
    lateinit var image: File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        val imagePath = intent.getStringExtra("image_path")

        if (imagePath != null) {
            image = File(imagePath)

            val imageView = findViewById<ImageView>(R.id.image1)
            if (image.exists()) {
                Log.d(TAG, "ConnectActivity/onCreate: ${image.path}, $imageView")
                imageView.setImageURI(Uri.fromFile(image))
            }
        }
        else {
            Toast.makeText(this, "err", Toast.LENGTH_SHORT).show()
        }
    }
}