package com.example.textrecognitionrnd


import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val STORAGE_CODE: Int = 100
    var imageBitmap: Bitmap? = null
    var selectedImage: Bitmap? = null
    lateinit var result: Task<Text>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCaptureImage?.setOnClickListener {
            dispatchTakePictureIntent()
            txtDisplay!!.text = ""
        }
        btnDetectTextImage?.setOnClickListener {
            if (selectedImage != null) {
                recognition()
                convertPDF()
            } else {
                Toast.makeText(this, "Please select a picture or take a picture", Toast.LENGTH_LONG)
                    .show()
            }
        }
        btnOpenImage?.setOnClickListener {
            openGallery()
        }
    }

    fun convertPDF() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_CODE
            )
        } else {
            savePDF()
        }
    }

    fun savePDF() {
        val document = PdfDocument()

        val pageInfo = PageInfo.Builder(300, 600, 1).create()

        val page: PdfDocument.Page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        paint.color = Color.RED
        paint.color = Color.BLACK

        val x: Float = 10F
        var y: Float = 25F
        for (line in txtDisplay.text.split("\n")) {
            canvas.drawText(line, x, y, paint)
            y += paint.descent() - paint.ascent()
        }
        document.finishPage(page)
        val mFileName = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(System.currentTimeMillis())
        val mFilePath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/" + mFileName

        val file = File(mFilePath)
        if (!file.exists()) {
            file.mkdirs()
        }
        val targetPdf = "$mFilePath.pdf"
        val filePath = File(targetPdf)
        try {
            document.writeTo(FileOutputStream(filePath))
            Toast.makeText(this, "Saved successfully. Path: ${filePath.path}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Log.e("main", "error " + e.toString())
            Toast.makeText(this, "Something wrong: " + e.toString(), Toast.LENGTH_LONG).show()
        }

        document.close()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    savePDF()
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun textRecognition() {
        val recognizer = TextRecognition.getClient()
        val image = InputImage.fromBitmap((imgRecognition.drawable as BitmapDrawable).bitmap, 0)
        result = recognizer.process(image)
            .addOnSuccessListener {
                txtDisplay!!.text = it.text
            }
            .addOnFailureListener {
                Log.e("Text:", it.message.toString())

            }
    }

    fun recognition() {
        val resultText = result.result.text
        for (block in result.result.textBlocks) {
            val blockText = block.text
            val blockCornerPoints = block.cornerPoints
            val blockFrame = block.boundingBox

            for (line in block.lines) {
                val lineText = line.text
                val lineCornerPoints = line.cornerPoints
                val lineFrame = line.boundingBox
                for (element in line.elements) {
                    val elementText = element.text
                    val elementCornerPoints = element.cornerPoints
                    val elementFrame = element.boundingBox
                }
            }

        }
        Log.e("resultText", resultText)
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun openGallery() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(photoPickerIntent, REQUEST_OPEN_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras = data?.extras
            imageBitmap = extras?.get("data") as Bitmap?
            imgRecognition.setImageBitmap(imageBitmap)
            textRecognition()
        } else if (requestCode == REQUEST_OPEN_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                val imageUri = data?.data
                val imageStream = imageUri?.let { contentResolver.openInputStream(it) }
                selectedImage = BitmapFactory.decodeStream(imageStream)
                imgRecognition.setImageBitmap(selectedImage)
                textRecognition()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_OPEN_IMAGE = 2
    }
}