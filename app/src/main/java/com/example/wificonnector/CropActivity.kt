package com.example.wificonnector

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageView
import com.example.wificonnector.R.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

const val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1

class CropActivity : AppCompatActivity() {
    lateinit var cropImageView: CropImageView
    lateinit var cancelBtn: Button
    lateinit var applyBtn: Button

    lateinit var image: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_crop)

        cropImageView = findViewById(id.CropImageView)
        cancelBtn = findViewById(id.cancelBtn)
        applyBtn = findViewById(id.applyBtn)

        val imagePath = intent.getStringExtra("image_path")

        if (imagePath != null) {
            image = File(imagePath)
            cropImageView.setImageUriAsync(Uri.fromFile(image))
        }

        applyBtn.setOnClickListener {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.visibility = View.VISIBLE
            cropImageView.visibility = View.INVISIBLE
            val cropping = CoroutineScope(Dispatchers.IO).launch {
                val cropped = cropImageView.getCroppedImage()
                saveCropped(cropped)
            }
            cropping.invokeOnCompletion {
                progressBar.visibility = View.INVISIBLE
            }
        }

        cancelBtn.setOnClickListener {
            finish()
        }
    }

    override fun onRestart() {
        super.onRestart()
        cropImageView.visibility = View.VISIBLE
    }

    private fun saveCropped(cropped: Bitmap?) {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir != null) {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val croppedimg = File(storageDir, "JPEG_${timeStamp}_.png")
            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(croppedimg)
                cropped?.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.close()
                connect(croppedimg.path)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this.applicationContext, "failed", Toast.LENGTH_SHORT).show()
                out?.close()
            }
        }
    }

    private fun connect(cropimg: String) {
        val intent = Intent(this, ConnectActivity::class.java).apply {
            putExtra("image_path", cropimg)
        }
        startActivity(intent)
    }
}