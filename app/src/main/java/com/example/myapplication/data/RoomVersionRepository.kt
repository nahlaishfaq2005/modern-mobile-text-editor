package com.example.myapplication.data

import com.example.myapplication.data.database.VersionDao
import com.example.myapplication.data.database.VersionEntity
import com.example.myapplication.data.database.DeltaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomVersionRepository(private val versionDao: VersionDao) : VersionRepository {

    override suspend fun saveVersion(version: Version) {
        withContext(Dispatchers.IO) {
            if (version.isCurrent) {
                versionDao.clearCurrentFlag(version.fileId)
            }
            versionDao.insertVersion(
                VersionEntity(
                    id = version.id,
                    fileId = version.fileId,
                    versionNumber = version.versionNumber,
                    name = version.name,
                    timestamp = version.timestamp,
                    parentVersionId = version.parentVersionId,
                    restoreSourceVersionId = version.restoreSourceVersionId,
                    isCurrent = version.isCurrent,
                    isAutoSaved = version.isAutoSaved
                )
            )
        }
    }

    override suspend fun saveVersionWithDelta(version: Version, delta: Delta) {
        withContext(Dispatchers.IO) {
            if (version.isCurrent) {
                versionDao.clearCurrentFlag(version.fileId)
            }
            versionDao.insertVersionWithDelta(
                VersionEntity(
                    id = version.id,
                    fileId = version.fileId,
                    versionNumber = version.versionNumber,
                    name = version.name,
                    timestamp = version.timestamp,
                    parentVersionId = version.parentVersionId,
                    restoreSourceVersionId = version.restoreSourceVersionId,
                    isCurrent = version.isCurrent,
                    isAutoSaved = version.isAutoSaved
                ),
                DeltaEntity(
                    id = delta.id,
                    versionId = delta.versionId,
                    patch = delta.patch
                )
            )
        }
    }

    override suspend fun getVersions(fileId: String): List<Version> = withContext(Dispatchers.IO) {
        versionDao.getVersionsForFile(fileId).map { entity ->
            Version(
                id = entity.id,
                fileId = entity.fileId,
                versionNumber = entity.versionNumber,
                name = entity.name,
                timestamp = entity.timestamp,
                parentVersionId = entity.parentVersionId,
                restoreSourceVersionId = entity.restoreSourceVersionId,
                isCurrent = entity.isCurrent,
                isAutoSaved = entity.isAutoSaved
            )
        }
    }

    override suspend fun getVersion(versionId: String): Version? = withContext(Dispatchers.IO) {
        versionDao.getVersionById(versionId)?.let { entity ->
            Version(
                id = entity.id,
                fileId = entity.fileId,
                versionNumber = entity.versionNumber,
                name = entity.name,
                timestamp = entity.timestamp,
                parentVersionId = entity.parentVersionId,
                restoreSourceVersionId = entity.restoreSourceVersionId,
                isCurrent = entity.isCurrent,
                isAutoSaved = entity.isAutoSaved
            )
        }
    }

    override suspend fun getLatestVersion(fileId: String): Version? = withContext(Dispatchers.IO) {
        versionDao.getLatestVersionForFile(fileId)?.let { entity ->
            Version(
                id = entity.id,
                fileId = entity.fileId,
                versionNumber = entity.versionNumber,
                name = entity.name,
                timestamp = entity.timestamp,
                parentVersionId = entity.parentVersionId,
                restoreSourceVersionId = entity.restoreSourceVersionId,
                isCurrent = entity.isCurrent,
                isAutoSaved = entity.isAutoSaved
            )
        }
    }

    override suspend fun deleteVersion(versionId: String) {
        withContext(Dispatchers.IO) {
            versionDao.deleteVersion(versionId)
        }
    }

    override suspend fun deleteAllVersions(fileId: String) {
        withContext(Dispatchers.IO) {
            versionDao.deleteAllVersionsForFile(fileId)
        }
    }

    override suspend fun getAllFileIdsWithVersions(): List<String> = withContext(Dispatchers.IO) {
        versionDao.getAllFileIdsWithVersions()
    }

    override suspend fun renameVersion(versionId: String, newName: String) {
        withContext(Dispatchers.IO) {
            versionDao.updateVersionName(versionId, newName)
        }
    }
}

class RoomDeltaRepository(private val versionDao: VersionDao) : DeltaRepository {

    override suspend fun saveDelta(delta: Delta) {
        withContext(Dispatchers.IO) {
            versionDao.insertDelta(
                DeltaEntity(
                    id = delta.id,
                    versionId = delta.versionId,
                    patch = delta.patch
                )
            )
        }
    }

    override suspend fun getDelta(versionId: String): Delta? = withContext(Dispatchers.IO) {
        versionDao.getDeltaForVersion(versionId)?.let { entity ->
            Delta(
                id = entity.id,
                versionId = entity.versionId,
                patch = entity.patch
            )
        }
    }

    override suspend fun deleteDelta(versionId: String) {
        withContext(Dispatchers.IO) {
            versionDao.deleteDeltaForVersion(versionId)
        }
    }
}
