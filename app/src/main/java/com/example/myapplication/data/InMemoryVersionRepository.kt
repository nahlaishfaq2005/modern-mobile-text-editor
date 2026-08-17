package com.example.myapplication.data

class InMemoryVersionRepository : VersionRepository {
    companion object {
        private val versions = mutableListOf<Version>()
    }

    override suspend fun saveVersion(version: Version) {
        versions.add(version)
    }

    override suspend fun saveVersionWithDelta(version: Version, delta: Delta) {
        versions.add(version)
        // InMemoryDeltaRepository is separate, but we could link them here if needed
    }
    
    override suspend fun getVersions(fileId: String): List<Version> {
        return versions.filter { it.fileId == fileId }
    }

    override suspend fun getVersion(versionId: String): Version? {
        return versions.find { it.id == versionId }
    }

    override suspend fun getLatestVersion(fileId: String): Version? {
        return versions.filter { it.fileId == fileId }.maxByOrNull { it.versionNumber }
    }

    override suspend fun deleteVersion(versionId: String) {
        versions.removeIf { it.id == versionId }
    }

    override suspend fun deleteAllVersions(fileId: String) {
        versions.removeIf { it.fileId == fileId }
    }

    override suspend fun getAllFileIdsWithVersions(): List<String> {
        return versions.map { it.fileId }.distinct()
    }

    override suspend fun renameVersion(versionId: String, newName: String) {
        val index = versions.indexOfFirst { it.id == versionId }
        if (index != -1) {
            versions[index] = versions[index].copy(name = newName)
        }
    }
}

class InMemoryDeltaRepository : DeltaRepository {
    companion object {
        private val deltas = mutableMapOf<String, Delta>()
    }

    override suspend fun saveDelta(delta: Delta) {
        deltas[delta.versionId] = delta
    }

    override suspend fun getDelta(versionId: String): Delta? {
        return deltas[versionId]
    }

    override suspend fun deleteDelta(versionId: String) {
        deltas.remove(versionId)
    }
}
