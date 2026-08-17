package com.example.myapplication.data

data class Version(
    val id: String,
    val fileId: String,
    val versionNumber: Int,
    val name: String?,
    val timestamp: Long,
    val parentVersionId: String?,
    val restoreSourceVersionId: String? = null,
    val isCurrent: Boolean = false,
    val isAutoSaved: Boolean = false
)

data class Delta(
    val id: String,
    val versionId: String,
    val patch: String // Serialized diff
)

enum class DiffType {
    UNCHANGED,
    ADDED,
    REMOVED,
    MODIFIED
}

data class DiffLine(
    val type: DiffType,
    val lineNumber: Int?,
    val content: String
)

interface VersionRepository {
    suspend fun saveVersion(version: Version) // Legacy for compatibility
    suspend fun saveVersionWithDelta(version: Version, delta: Delta)
    suspend fun getVersions(fileId: String): List<Version>
    suspend fun getVersion(versionId: String): Version?
    suspend fun getLatestVersion(fileId: String): Version?
    suspend fun deleteVersion(versionId: String)
    suspend fun deleteAllVersions(fileId: String)
    suspend fun getAllFileIdsWithVersions(): List<String>
    suspend fun renameVersion(versionId: String, newName: String)
}

interface DeltaRepository {
    suspend fun saveDelta(delta: Delta)
    suspend fun getDelta(versionId: String): Delta?
    suspend fun deleteDelta(versionId: String)
}
