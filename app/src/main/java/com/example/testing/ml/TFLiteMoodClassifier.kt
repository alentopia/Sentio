package com.example.testing.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

class TFLiteMoodClassifier(context: Context) {

    private val interpreter: Interpreter
    private val inputHeight: Int
    private val inputWidth: Int
    private val inputChannels: Int
    private val numClasses: Int

    init {
        val model = FileUtil.loadMappedFile(context, "fer_cnn_classifier_6class_uint8.tflite")
        interpreter = Interpreter(model)

        // baca shape input & output
        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()  // [1,H,W,C]
        inputHeight = inputShape[1]
        inputWidth = inputShape[2]
        inputChannels = inputShape[3]

        val outputTensor = interpreter.getOutputTensor(0)
        numClasses = outputTensor.shape()[1]  // [1,6]
    }

    fun predict(bitmap: Bitmap): Int {
        val resized = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

        val input = Array(1) {
            Array(inputHeight) {
                Array(inputWidth) {
                    ByteArray(inputChannels)
                }
            }
        }

        for (y in 0 until inputHeight) {
            for (x in 0 until inputWidth) {
                val pixel = resized.getPixel(x, y)

                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                if (inputChannels == 1) {
                    val gray = ((r + g + b) / 3).toByte()
                    input[0][y][x][0] = gray
                } else {
                    input[0][y][x][0] = r.toByte()
                    if (inputChannels > 1) input[0][y][x][1] = g.toByte()
                    if (inputChannels > 2) input[0][y][x][2] = b.toByte()
                }
            }
        }

        // OUTPUT = UINT8
        val output = Array(1) { ByteArray(numClasses) }
        interpreter.run(input, output)
        val probs = output[0].map { it.toInt() and 0xFF }
        return probs.indices.maxBy { probs[it] }
    }
}
