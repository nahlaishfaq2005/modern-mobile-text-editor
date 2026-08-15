package com.example.myapplication.data.database

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils

/**
 * Manager responsible for calculating deltas/patches between text states.
 * Uses java-diff-utils for efficient line-based diffing.
 */
object DeltaManager {

    /**
     * Calculates a delta (patch) between two strings.
     * 
     * @param previousText The base text
     * @param currentText The updated text
     * @return A string representation of the patch in unified diff format.
     */
    fun createDelta(previousText: String, currentText: String): String {
        val original = previousText.lines()
        val revised = currentText.lines()

        val patch = DiffUtils.diff(original, revised)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
            "original", 
            "revised", 
            original, 
            patch, 
            0
        )
        
        return unifiedDiff.joinToString("\n")
    }
}
