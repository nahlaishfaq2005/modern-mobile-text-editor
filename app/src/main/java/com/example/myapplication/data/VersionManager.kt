package com.example.myapplication.data

import java.util.*

class VersionManager(
    private val versionRepository: VersionRepository,
    private val deltaRepository: DeltaRepository,
    private val diffManager: DiffManager
) {

    suspend fun createVersion(
        fileId: String,
        content: String,
        name: String? = null,
        isAutoSaved: Boolean = false,
        restoreSourceVersionId: String? = null
    ): VersionResult {
        val latestVersion = versionRepository.getLatestVersion(fileId)
        val previousContent = if (latestVersion != null) {
            reconstructContent(fileId, latestVersion.id)
        } else {
            ""
        }

        // Task 25: Prevent duplicate versions (Only for auto-saves)
        if (isAutoSaved && latestVersion != null && content == previousContent) {
            return VersionResult.Duplicate
        }

        val versionNumber = (latestVersion?.versionNumber ?: 0) + 1
        val versionId = UUID.randomUUID().toString()
        val version = Version(
            id = versionId,
            fileId = fileId,
            versionNumber = versionNumber,
            name = name,
            timestamp = System.currentTimeMillis(),
            parentVersionId = latestVersion?.id,
            restoreSourceVersionId = restoreSourceVersionId,
            isCurrent = true,
            isAutoSaved = isAutoSaved
        )

        // Task 24: Incremental Delta-Based Versioning
        // Store delta from parent to this version
        val deltaString = diffManager.calculateDelta(previousContent, content)
        val delta = Delta(
            id = UUID.randomUUID().toString(),
            versionId = versionId,
            patch = deltaString
        )

        versionRepository.saveVersionWithDelta(version, delta)

        return VersionResult.Success(version)
    }

    suspend fun reconstructContent(fileId: String, targetVersionId: String): String {
        val versions = versionRepository.getVersions(fileId).sortedBy { it.versionNumber }
        val targetVersionIndex = versions.indexOfFirst { it.id == targetVersionId }
        
        if (targetVersionIndex == -1) return ""

        var currentContent = ""
        for (i in 0..targetVersionIndex) {
            val v = versions[i]
            val delta = deltaRepository.getDelta(v.id)
            if (delta != null) {
                try {
                    currentContent = diffManager.applyDelta(currentContent, delta.patch)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If one delta fails, we might have to stop or return what we have
                }
            }
        }
        
        return currentContent
    }

    suspend fun getVersionCount(fileId: String): Int {
        return versionRepository.getVersions(fileId).size
    }

    suspend fun getVersionHistory(fileId: String): List<Version> {
        return versionRepository.getVersions(fileId).sortedByDescending { it.versionNumber }
    }

    suspend fun getAllFilesWithVersions(): List<String> {
        return versionRepository.getAllFileIdsWithVersions()
    }

    suspend fun deleteVersion(versionId: String) {
        versionRepository.deleteVersion(versionId)
        deltaRepository.deleteDelta(versionId)
    }

    suspend fun renameVersion(versionId: String, newName: String) {
        versionRepository.renameVersion(versionId, newName)
    }

    suspend fun deleteAllVersions(fileId: String) {
        val versions = versionRepository.getVersions(fileId)
        versions.forEach { 
            deltaRepository.deleteDelta(it.id)
        }
        versionRepository.deleteAllVersions(fileId)
    }

    sealed class VersionResult {
        data class Success(val version: Version) : VersionResult()
        object Duplicate : VersionResult()
        data class Error(val message: String) : VersionResult()
    }
}
