package com.example.wificonnector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

class ConnectActivity : AppCompatActivity() {
    private lateinit var image: File
    private lateinit var AP: Spinner
    private lateinit var wifiManager: WifiManager
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var results: List<ScanResult>
    private lateinit var PW: EditText
    private lateinit var connect: ConstraintLayout
    private lateinit var loading: LinearLayout
    private lateinit var btnConnect: Button
    private lateinit var btnCancel: Button
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        val imagePath = intent.getStringExtra("image_path")
        AP = findViewById(R.id.AP)
        PW = findViewById(R.id.PW)
        connect = findViewById(R.id.connect)
        loading = findViewById(R.id.loading)
        btnCancel = findViewById(R.id.btnCancel)
        btnConnect = findViewById(R.id.btnConnect)
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        connect.visibility = View.INVISIBLE

        Log.d(TAG, "onCreate: received: $imagePath")

        if (imagePath != null) {
            image = File(imagePath)

            val imageView = findViewById<ImageView>(R.id.image1)
            if (image.exists()) {
                imageView.setImageURI(Uri.fromFile(image))
                getPredict(imagePath)
            }
        }
        else {
            Toast.makeText(this, "오류: 이미지를 전달받지 못함!", Toast.LENGTH_SHORT).show()
        }

        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "WiFi 활성화 중", Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled = true
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION)
        } else {
            lifecycleScope.launch {
                scanWifi()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnConnect.setOnClickListener {
            val SSID = AP.selectedItem as String
            val Pwd = PW.text.toString()
            suggestNetwork(SSID, Pwd)
        }

    }

    private suspend fun scanWifi() = withContext(Dispatchers.IO) {
        val success = wifiManager.startScan()
        if (!success) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ConnectActivity, "WiFi 검색 실패", Toast.LENGTH_SHORT).show()
            }
            return@withContext
        }

        results = getWifiScanResults()
        val deviceList = results.map { it.SSID }.filter { it.isNotEmpty() }

        withContext(Dispatchers.Main) {
            adapter = ArrayAdapter(this@ConnectActivity, android.R.layout.simple_spinner_item, deviceList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            AP.adapter = adapter
            loading.visibility = View.INVISIBLE
            connect.visibility = View.VISIBLE
        }
    }

    private suspend fun getWifiScanResults(): List<ScanResult> = suspendCancellableCoroutine { continuation ->
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val results = wifiManager.scanResults
                    unregisterReceiver(this)
                    continuation.resume(results)
                }
            }
        }

        registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        continuation.invokeOnCancellation {
            unregisterReceiver(receiver)
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            results = wifiManager.scanResults
            unregisterReceiver(this)

            val deviceList = results.map { it.SSID }.filter { it.isNotEmpty() }

            adapter = ArrayAdapter(this@ConnectActivity, android.R.layout.simple_spinner_item, deviceList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            AP.adapter = adapter
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun suggestNetwork(ssid: String, password: String) {
        val suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val suggestionsList = listOf(suggestion)
        val status = wifiManager.addNetworkSuggestions(suggestionsList)

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Toast.makeText(this, "추가됨: $ssid", Toast.LENGTH_SHORT).show()
        } else {
            wifiManager.removeNetworkSuggestions(suggestionsList)
            val retryStatus = wifiManager.addNetworkSuggestions(suggestionsList)
            if (retryStatus == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                Toast.makeText(this, "추가됨: $ssid", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "$ssid: 추가 실패", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to add network suggestion: $retryStatus")
            }
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
                PW.setText(resultData)
            }
        }

        resultLauncher.launch(intent)

        return resultData
    }
}