package com.example.myapplication.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

class FileRepository(private val context: Context) {

    /**
     * Saves the content to the specified file using UTF-8 encoding.
     * Task 4 & 5 Requirement: Use UTF-8 and handle English, Numbers, Symbols, Unicode (Sinhala etc.)
     */
    fun saveFile(fileName: String, content: String): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            // Use UTF-8 explicitly to handle all Unicode characters
            outputStream.write(content.toByteArray(StandardCharsets.UTF_8))
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Loads the content of a file.
     */
    fun loadFile(fileName: String): String {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return ""
            // readText defaults to UTF-8
            file.readText(StandardCharsets.UTF_8)
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

    /**
     * Checks if a file with the given name already exists.
     * Useful for Save As validation.
     */
    fun fileExists(fileName: String): Boolean {
        return File(context.filesDir, fileName).exists()
    }

    /**
     * Deletes the specified file.
     */
    fun deleteFile(fileName: String): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
