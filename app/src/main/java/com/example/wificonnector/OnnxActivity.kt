package com.example.wificonnector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ai.onnxruntime.*
import android.content.Intent
import android.widget.Toast
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer

class OnnxActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 모델 파일 복사
        val modelFile = File(filesDir, "model.onnx")
        if (!modelFile.exists()) {
            assets.open("model.onnx").use { inputStream ->
                modelFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        // 이미지 파일 로드
        val imagePath: String = intent.getStringExtra("image_path").toString()
        val bitmap = BitmapFactory.decodeStream(File(imagePath).inputStream()).convertToGrayscale()
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 100, 32, true)

        // 이미지와 텍스트 입력 텐서 준비
        val imgTensor = bitmapToFloatBuffer(resizedBitmap)
        val textTensor = LongArray(26) { 0 } // 1 + 25, 텍스트 입력 텐서 초기화

        // ONNX 런타임 세션 생성 및 추론 수행
        val ortEnvironment = OrtEnvironment.getEnvironment()
        val session = ortEnvironment.createSession(modelFile.absolutePath)

        val inputNameImg = session.inputNames.elementAt(0)
        val inputNameText = session.inputNames.elementAt(1)

        val imgShape = longArrayOf(1, 1, 32, 100) // [1, 1, imgH, imgW]
        val textShape = longArrayOf(1, 26) // [1, batch_max_length + 1]

        val imgInputTensor = OnnxTensor.createTensor(ortEnvironment, imgTensor, imgShape)
        val textInputTensor = OnnxTensor.createTensor(ortEnvironment, LongBuffer.wrap(textTensor), textShape)

        val inputs = mapOf(inputNameImg to imgInputTensor, inputNameText to textInputTensor)

        val results = session.run(inputs)
        val output = results[0].value as Array<Array<FloatArray>>

        // 결과를 인덱스로 변환
        val indexes = convertOutputToIndexes(output[0])

        // 결과 디코딩 및 출력
        val preds = decodeOutput(indexes, 25).replace("[s]", "") // batch_max_length
        Toast.makeText(this, preds, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK, Intent().apply { putExtra("password", preds) })
        finish()
    }

    private fun Bitmap.convertToGrayscale(): Bitmap {
        val width = this.width
        val height = this.height
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = this.getPixel(x, y)
                val red = (pixel shr 16 and 0xFF)
                val green = (pixel shr 8 and 0xFF)
                val blue = (pixel and 0xFF)
                val gray = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
                grayBitmap.setPixel(x, y, 0xFF shl 24 or (gray shl 16) or (gray shl 8) or gray)
            }
        }
        return grayBitmap
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap): FloatBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val floatBuffer = FloatBuffer.allocate(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val red = (pixel shr 16 and 0xFF) / 255.0f
                floatBuffer.put(red)
            }
        }
        floatBuffer.rewind()
        return floatBuffer
    }

    private fun convertOutputToIndexes(output: Array<FloatArray>): Array<LongArray> {
        return Array(1) { _ ->
            LongArray(output.size) { i ->
                output[i].toList().indexOf(output[i].maxOrNull() ?: Float.MIN_VALUE).toLong()
            }
        }
    }

    private fun decodeOutput(output: Array<LongArray>, batchMaxLength: Int): String {
        val characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!\"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~"
        val converter = AttnLabelConverter(characters)  // Converter 객체 생성

        // 길이 텐서 생성
        val lengthForPred = IntArray(1) { batchMaxLength }

        // output 배열 내용 로그로 출력
        for (i in output.indices) {
            Log.d("decodeOutput", "output[$i]: ${output[i].joinToString(", ")}")
        }

        // 디코딩 수행
        val predsStr = converter.decode(output, lengthForPred)

        // 결과 반환
        return predsStr.joinToString(separator = ", ")
    }

}
