package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.IOException

class ImageClassifierHelper(
    private val context: Context,
    private val listener: ClassifierListener? = null
) {

    private var imageClassifier: ImageClassifier? = null

    private fun setupImageClassifier() {
        try {
            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(3)
                .setScoreThreshold(0.1f)
                .build()
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                "cancer_classification.tflite",
                options
            )
        } catch (e: Exception) {
            Log.e("ImageClassifierHelper", "Error initializing classifier: ${e.message}")
            listener?.onError("Failed to set up image classifier.")
        }
    }

    fun classifyStaticImage(imageUri: Uri) {
        setupImageClassifier()

        if (imageClassifier == null) {
            listener?.onError("Image classifier not set up properly.")
            return
        }

        try {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
                }.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri).copy(Bitmap.Config.ARGB_8888, true)
            }
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val startTime = SystemClock.uptimeMillis()
            val results = imageClassifier?.classify(tensorImage)
            val inferenceTime = SystemClock.uptimeMillis() - startTime
            listener?.onResults(results ?: emptyList(), inferenceTime)

        } catch (e: IOException) {
            listener?.onError("Failed to load image.")
            Log.e("ImageClassifierHelper", "Error loading image: ${e.message}")
        }
    }


    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(results: List<Classifications>, inferenceTime: Long)
    }
}
