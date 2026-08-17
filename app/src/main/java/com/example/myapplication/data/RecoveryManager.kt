package com.example.myapplication.data

import android.content.Context
import com.google.gson.Gson
import java.io.File

class RecoveryManager(context: Context) {
    private val recoveryDir = File(context.cacheDir, "recovery")
    private val gson = Gson()

    init {
        if (!recoveryDir.exists()) {
            recoveryDir.mkdirs()
        }
    }

    fun cacheActiveFile(fileName: String, content: String, isModified: Boolean) {
        val recoveryData = RecoveryData(
            fileName = fileName,
            content = content,
            lastCacheTime = System.currentTimeMillis(),
            isModified = isModified,
        )
        val recoveryFile = getRecoveryFile(fileName)
        recoveryFile.writeText(gson.toJson(recoveryData))
    }

    fun hasRecoveryData(fileName: String): Boolean {
        val recoveryFile = getRecoveryFile(fileName)
        return recoveryFile.exists()
    }

    fun getRecoveryData(fileName: String): RecoveryData? {
        val recoveryFile = getRecoveryFile(fileName)
        return if (recoveryFile.exists()) {
            try {
                gson.fromJson(recoveryFile.readText(), RecoveryData::class.java)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun deleteRecoveryFile(fileName: String) {
        val recoveryFile = getRecoveryFile(fileName)
        if (recoveryFile.exists()) {
            recoveryFile.delete()
        }
    }

    private fun getRecoveryFile(fileName: String): File {
        // Sanitize filename for use as a file name
        val sanitized = fileName.replace(File.separator, "_").replace("/", "_")
        return File(recoveryDir, "$sanitized.recovery.json")
    }
}
