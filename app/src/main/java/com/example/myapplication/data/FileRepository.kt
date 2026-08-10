package com.example.myapplication.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class FileRepository(private val context: Context) {

    fun saveFile(fileName: String, content: String): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            outputStream.write(content.toByteArray())
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadFile(fileName: String): String {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return ""
            file.readText()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
