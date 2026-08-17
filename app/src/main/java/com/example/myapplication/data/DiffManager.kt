package com.example.myapplication.data

import com.github.difflib.DiffUtils
import com.github.difflib.patch.AbstractDelta
import com.github.difflib.patch.DeltaType
import com.github.difflib.patch.Patch
import com.google.gson.Gson

class DiffManager {
    private val gson = Gson()

    fun calculateDelta(oldText: String, newText: String): String {
        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val newLines = if (newText.isEmpty()) emptyList() else newText.lines()
        val patch = DiffUtils.diff(oldLines, newLines)
        
        val deltaData = patch.deltas.map { delta ->
            SerializedDelta(
                type = delta.type.name,
                sourcePosition = delta.source.position,
                sourceLines = delta.source.lines,
                targetPosition = delta.target.position,
                targetLines = delta.target.lines
            )
        }
        return gson.toJson(deltaData)
    }

    fun applyDelta(oldText: String, deltaString: String): String {
        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val deltaData = gson.fromJson(deltaString, Array<SerializedDelta>::class.java) ?: emptyArray()
        
        val patch = Patch<String>()
        deltaData.forEach { data ->
            val deltaType = try { DeltaType.valueOf(data.type) } catch(e: Exception) { DeltaType.CHANGE }
            val delta = when (deltaType) {
                DeltaType.CHANGE -> com.github.difflib.patch.ChangeDelta(
                    com.github.difflib.patch.Chunk(data.sourcePosition, data.sourceLines),
                    com.github.difflib.patch.Chunk(data.targetPosition, data.targetLines)
                )
                DeltaType.DELETE -> com.github.difflib.patch.DeleteDelta(
                    com.github.difflib.patch.Chunk(data.sourcePosition, data.sourceLines),
                    com.github.difflib.patch.Chunk(data.targetPosition, data.targetLines)
                )
                DeltaType.INSERT -> com.github.difflib.patch.InsertDelta(
                    com.github.difflib.patch.Chunk(data.sourcePosition, data.sourceLines),
                    com.github.difflib.patch.Chunk(data.targetPosition, data.targetLines)
                )
                else -> null
            }
            if (delta != null) patch.addDelta(delta)
        }
        
        val newLines = DiffUtils.patch(oldLines, patch)
        return newLines.joinToString("\n")
    }

    fun getDiffResult(oldText: String, newText: String): List<DiffLine> {
        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val newLines = if (newText.isEmpty()) emptyList() else newText.lines()
        val patch = DiffUtils.diff(oldLines, newLines)
        
        val result = mutableListOf<DiffLine>()
        
        // This is a simplified reconstruction for display
        // In a real app, you'd iterate through both and match them
        // For Task 28, we need line-by-line differences
        
        val deltas = patch.deltas.sortedBy { it.source.position }
        var currentOldLine = 0
        
        deltas.forEach { delta ->
            // Add unchanged lines before the delta
            while (currentOldLine < delta.source.position) {
                result.add(DiffLine(DiffType.UNCHANGED, currentOldLine + 1, oldLines[currentOldLine]))
                currentOldLine++
            }
            
            when (delta.type) {
                DeltaType.INSERT -> {
                    delta.target.lines.forEachIndexed { index, line ->
                        result.add(DiffLine(DiffType.ADDED, null, line))
                    }
                }
                DeltaType.DELETE -> {
                    delta.source.lines.forEachIndexed { index, line ->
                        result.add(DiffLine(DiffType.REMOVED, currentOldLine + 1 + index, line))
                    }
                    currentOldLine += delta.source.lines.size
                }
                DeltaType.CHANGE -> {
                    delta.source.lines.forEachIndexed { index, line ->
                        result.add(DiffLine(DiffType.REMOVED, currentOldLine + 1 + index, line))
                    }
                    delta.target.lines.forEachIndexed { index, line ->
                        result.add(DiffLine(DiffType.ADDED, null, line))
                    }
                    currentOldLine += delta.source.lines.size
                }
                else -> {}
            }
        }
        
        // Add remaining unchanged lines
        while (currentOldLine < oldLines.size) {
            result.add(DiffLine(DiffType.UNCHANGED, currentOldLine + 1, oldLines[currentOldLine]))
            currentOldLine++
        }
        
        return result
    }

    private data class SerializedDelta(
        val type: String,
        val sourcePosition: Int,
        val sourceLines: List<String>,
        val targetPosition: Int,
        val targetLines: List<String>
    )
}
