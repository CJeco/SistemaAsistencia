package com.example.sistemadeasistencia

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object QRUtils {

   fun generarCodigoQR(texto: String): Bitmap? {
       return try {
           val barcodeEncoder = BarcodeEncoder()
           barcodeEncoder.encodeBitmap(texto, BarcodeFormat.QR_CODE, 500, 500)
       }catch (e: Exception){
           e.printStackTrace()
           null
       }
   }

    fun guardarImagenEnGaleria(context: Context, bitmap: Bitmap, nombre: String): Boolean {
        val filename = "$nombre.png"
        var outputStream: OutputStream? = null
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DocentesQR")
                }
                val contentResolver = context.contentResolver
                uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { contentResolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val image = File(imagesDir, filename)
                outputStream = FileOutputStream(image)
            }

            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

}