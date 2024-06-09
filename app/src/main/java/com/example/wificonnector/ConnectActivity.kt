package com.example.wificonnector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
                getPredict(imagePath)
            }
        }
        else {
            Toast.makeText(this, "err", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPredict(imagePath: String): String {
        var resultData = ""

        val intent = Intent(this, OnnxActivity::class.java).apply {
            putExtra("image_path", imagePath)
        }
        var resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                resultData = data?.getStringExtra("password").toString()
                Log.d("password", "$resultData")
            }
        }

        resultLauncher.launch(intent)

        return resultData
    }
}