package com.example.recycleme

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.widget.TextView
import android.widget.Toast
import com.example.recycleme.ml.ModelUnquant
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min


class ScannerFragment : Fragment() {
    lateinit var btnUpload: Button
    lateinit var btnScan: Button
    lateinit var itemImage: ImageView
    lateinit var bitmap: Bitmap
    private val imageSize = 224

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_scanner, container, false)

        btnScan = view.findViewById(R.id.btnScan)
        btnUpload = view.findViewById(R.id.btnUpload)
        itemImage = view.findViewById(R.id.itemImage)

        btnScan.setOnClickListener {
            // Check for CAMERA permission using ContextCompat
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 1)
            } else {
                // Request camera permission if we don't have it.
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
            }
        }

        btnUpload.setOnClickListener {
            var intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent,100)
        }

        return view

    }

    fun classifyImage(image: Bitmap) {

        val model = ModelUnquant.newInstance(requireActivity().applicationContext)

        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(imageSize * imageSize)
        image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
        var pixel = 0
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val valPixel = intValues[pixel++]
                byteBuffer.putFloat(((valPixel shr 16 and 0xFF) * (1.0f / 255.0f)))
                byteBuffer.putFloat(((valPixel shr 8 and 0xFF) * (1.0f / 255.0f)))
                byteBuffer.putFloat((valPixel and 0xFF) * (1.0f / 255.0f))
            }
        }


        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer


        val confidences = outputFeature0.getFloatArray()
        var maxPos = 0
        var maxConfidence = 0f

        for (i in confidences.indices) {
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i]
                maxPos = i
            }
        }

        val classes = arrayOf("Paper - Recyclable", "Plastic - Recyclable",
                               "Metal - Recyclable","Glass - Recyclable",
                                "Ewaste - Recyclable","Paper - Non Recyclable",
                                "Plastic - Non Recyclable","Metal - Non Recyclable",
                                 "Glass - Non Recyclable", "Others - Non Recyclable")


        println("ML RESULTS")
        println(classes[maxPos])

        var s = ""

        for (i in classes.indices) {
            s += "${classes[i]}: ${String.format("%.1f%%", confidences[i] * 100)}\n"
        }

        println(s)

        // Releases model resources if no longer used.
        model.close()

        val data = s
        val maxPosValue = classes[maxPos]
        val resultsFragment = ScannerResultsFragment.newInstance(data, maxPosValue)

        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.container, resultsFragment)
        transaction.addToBackStack(null) // If you want to add this transaction to the back stack
        transaction.commit()




    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                if (bitmap != null) {
                    val dimension = min(bitmap.width, bitmap.height)
                    var processedImage = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension)
                    itemImage.setImageBitmap(processedImage)

                    // Resize the image to imageSize x imageSize
                    processedImage = Bitmap.createScaledBitmap(processedImage, imageSize, imageSize, false)

                    // Classify the image
                    classifyImage(processedImage)
                } else {
                    // Where the loaded bitmap is null
                    // Print an error message or show a notification
                    println("Loaded bitmap is null")
                }
            } else {
                // Handle the case where the URI is null
                // Print an error message or show a notification
                println("URI is null")
            }
        } else if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            val image = data?.extras?.get("data") as Bitmap?
            val dimension = min(image?.width ?: 0, image?.height ?: 0)
            var processedImage = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
            itemImage.setImageBitmap(processedImage)

            // Resize the image to imageSize x imageSize
            processedImage = Bitmap.createScaledBitmap(processedImage, imageSize, imageSize, false)
            println("processedImage")
            println(processedImage) // Print a simple string
            classifyImage(processedImage)

        }
    }
}