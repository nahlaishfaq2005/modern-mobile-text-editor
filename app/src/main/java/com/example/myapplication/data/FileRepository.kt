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

    /**
     * Saves an immutable base snapshot of a file.
     * This is only done once when Version 1 is created.
     */
    fun saveBaseSnapshot(fileId: Long, content: String): Boolean {
        return try {
            val baseDir = File(context.filesDir, "version_bases")
            if (!baseDir.exists()) baseDir.mkdirs()
            
            val baseFile = File(baseDir, "${fileId}_base.txt")
            if (baseFile.exists()) return true // Already exists, do not overwrite
            
            baseFile.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Loads the immutable base snapshot for a file.
     */
    fun loadBaseSnapshot(fileId: Long): String? {
        return try {
            val baseFile = File(context.filesDir, "version_bases/${fileId}_base.txt")
            if (!baseFile.exists()) return null
            baseFile.readText()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
