package com.example.myapplication.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.Serializable

class RecoveryManager(private val context: Context) {
    private val TAG = "RecoveryManager"

    fun cacheActiveFile(fileName: String, content: String, isModified: Boolean, filePath: String? = null) {
        try {
            val recoveryFileName = getRecoveryFileName(fileName)
            val recoveryFile = File(context.cacheDir, recoveryFileName)
            
            val currentTime = System.currentTimeMillis()
            val metadata = "$fileName|$currentTime|$isModified|${filePath ?: ""}\n"
            recoveryFile.writeText(metadata + content)
            
            Log.d(TAG, "Cached $fileName to $recoveryFileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching file: ${e.message}")
        }
    }

    private fun getRecoveryFileName(fileName: String): String {
        return if (fileName.isEmpty() || fileName == "Untitled") {
            "Untitled_1.recovery"
        } else {
            "$fileName.recovery"
        }
    }

    fun getAllRecoveryFiles(): List<RecoveryData> {
        val recoveryFiles = context.cacheDir.listFiles { _, name -> name.endsWith(".recovery") } ?: return emptyList()
        return recoveryFiles.mapNotNull { file ->
            try {
                val allText = file.readText()
                val firstNewLine = allText.indexOf('\n')
                if (firstNewLine == -1) return@mapNotNull null
                
                val metadataLine = allText.substring(0, firstNewLine)
                val content = allText.substring(firstNewLine + 1)
                
                val parts = metadataLine.split("|")
                if (parts.size < 3) return@mapNotNull null
                
                RecoveryData(
                    fileName = parts[0],
                    lastCacheTime = parts[1].toLongOrNull() ?: 0L,
                    isModified = parts[2].toBoolean(),
                    filePath = if (parts.size > 3) parts[3].ifEmpty { null } else null,
                    content = content
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getRecoveryData(fileName: String): RecoveryData? {
        try {
            val recoveryFileName = getRecoveryFileName(fileName)
            val recoveryFile = File(context.cacheDir, recoveryFileName)
            
            if (!recoveryFile.exists()) return null
            
            val allText = recoveryFile.readText()
            val firstNewLine = allText.indexOf('\n')
            if (firstNewLine == -1) return null
            
            val metadataLine = allText.substring(0, firstNewLine)
            val content = allText.substring(firstNewLine + 1)
            
            val parts = metadataLine.split("|")
            if (parts.size < 3) return null
            
            return RecoveryData(
                fileName = parts[0],
                lastCacheTime = parts[1].toLongOrNull() ?: 0L,
                isModified = parts[2].toBoolean(),
                filePath = if (parts.size > 3) parts[3].ifEmpty { null } else null,
                content = content
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading recovery data: ${e.message}")
            return null
        }
    }

    fun deleteRecoveryFile(fileName: String) {
        val recoveryFileName = getRecoveryFileName(fileName)
        val recoveryFile = File(context.cacheDir, recoveryFileName)
        if (recoveryFile.exists()) {
            recoveryFile.delete()
        }
    }
    
    fun hasRecoveryData(fileName: String): Boolean {
        val recoveryFileName = getRecoveryFileName(fileName)
        return File(context.cacheDir, recoveryFileName).exists()
    }

    companion object {
        private const val RECOVERY_EXTENSION = ".recovery"
    }
}
