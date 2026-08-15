package com.example.myapplication.data.database

import android.util.Log
import com.example.myapplication.data.FileRepository
import com.example.myapplication.data.database.entity.VersionEntity
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils

/**
 * Manager responsible for reconstructing historical versions of files.
 * Uses the base file content and sequential patches to rebuild a version.
 */
class ReconstructionManager(
    private val database: AppDatabase,
    private val fileRepository: FileRepository
) {

    private val TAG = "ReconstructionManager"

    /**
     * Reconstructs the text of a specific version for a file.
     * 
     * @param fileId The ID of the file.
     * @param targetVersionId The ID of the version to reconstruct.
     * @return The reconstructed text, or null if reconstruction fails.
     */
    suspend fun reconstructVersion(fileId: Long, targetVersionId: Long): String? {
        return try {
            val versionDao = database.versionDao()
            val deltaDao = database.deltaDao()
            val fileDao = database.fileDao()

            // 1. Find the requested VersionEntity
            val targetVersion = versionDao.getVersionById(targetVersionId)
            if (targetVersion == null || targetVersion.fileId != fileId) {
                Log.e(TAG, "Target version not found: $targetVersionId for file $fileId")
                return null
            }

            // 2. Load FileEntity to get the file name/path
            val fileEntity = fileDao.getFileById(fileId)
            if (fileEntity == null) {
                Log.e(TAG, "FileEntity not found: $fileId")
                return null
            }

            // 3. Build the version chain from target back to base (Version 1)
            val versionChain = mutableListOf<VersionEntity>()
            var current: VersionEntity? = targetVersion
            while (current != null) {
                versionChain.add(current)
                if (current.versionNumber == 1) break
                
                val parentId = current.parentVersionId
                current = if (parentId != null) {
                    versionDao.getVersionById(parentId)
                } else {
                    // Fallback to sequential if parentVersionId is missing
                    versionDao.getVersionByNumber(fileId, current.versionNumber - 1)
                }
            }

            if (versionChain.isEmpty() || versionChain.last().versionNumber != 1) {
                Log.e(TAG, "Could not find a complete version chain back to base for $targetVersionId")
                return null
            }
            
            val orderedVersions = versionChain.reversed()

            // 4. Load immutable base content (Version 1)
            val baseContent = fileRepository.loadBaseSnapshot(fileId)
            if (baseContent == null) {
                Log.e(TAG, "Base snapshot not found for file $fileId")
                return null
            }
            var currentLines = baseContent.lines()

            // 5. Apply patches sequentially from Version 2 up to Target Version
            for (version in orderedVersions) {
                // Version 1 is the base snapshot already loaded
                if (version.versionNumber == 1) continue

                // Find the delta for this version
                val delta = deltaDao.getDeltaForVersion(version.id)
                if (delta == null) {
                    Log.e(TAG, "Delta not found for version: ${version.versionNumber}")
                    return null
                }

                // Parse and apply the patch
                try {
                    val patchLines = delta.patchText.lines()
                    val patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
                    currentLines = DiffUtils.patch(currentLines, patch)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to apply patch for version ${version.versionNumber}: ${e.message}")
                    return null
                }
            }

            // 6. Return reconstructed text
            currentLines.joinToString("\n")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during reconstruction: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
