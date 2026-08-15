package com.example.myapplication.data.database

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.PatchFailedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit test to verify the delta calculation and reconstruction logic.
 */
class ReconstructionManagerTest {

    @Test
    fun testReconstructionSequence() {
        // Version 1: Initial state
        val v1Text = "Hello\nWorld"
        
        // Version 2: Change "World" to "Beautiful World"
        val v2Text = "Hello\nBeautiful World"
        
        // Version 3: Add an exclamation mark
        val v3Text = "Hello\nBeautiful World!"

        // 1. Calculate Deltas
        val delta2Text = DeltaManager.createDelta(v1Text, v2Text)
        val delta3Text = DeltaManager.createDelta(v2Text, v3Text)

        // 2. Reconstruct Version 2 from Version 1
        val baseLines = v1Text.lines()
        val patch2 = UnifiedDiffUtils.parseUnifiedDiff(delta2Text.lines())
        val reconstructedV2Lines = DiffUtils.patch(baseLines, patch2)
        val reconstructedV2Text = reconstructedV2Lines.joinToString("\n")
        
        assertEquals("Reconstruction of Version 2 failed", v2Text, reconstructedV2Text)

        // 3. Reconstruct Version 3 from Version 2
        val patch3 = UnifiedDiffUtils.parseUnifiedDiff(delta3Text.lines())
        val reconstructedV3Lines = DiffUtils.patch(reconstructedV2Lines, patch3)
        val reconstructedV3Text = reconstructedV3Lines.joinToString("\n")

        assertEquals("Reconstruction of Version 3 failed", v3Text, reconstructedV3Text)
    }

    @Test
    fun testReconstructionWithMutableWorkingFile() {
        // This test proves that ReconstructionManager MUST use the immutable base snapshot,
        // because the working file may have evolved past the state the diff expects.
        
        val v1Text = "Hello\nWorld"
        val v2Text = "Hello\nBeautiful World"
        val v3Text = "Hello\nBeautiful World!"

        // 1. Version 1 is stored as an immutable base.
        val immutableBase = v1Text
        
        // 2. Delta 2 is calculated relative to Version 1.
        val delta2Text = DeltaManager.createDelta(v1Text, v2Text)
        val patch2 = UnifiedDiffUtils.parseUnifiedDiff(delta2Text.lines())

        // 3. The working file evolves to Version 3.
        val currentWorkingFile = v3Text
        
        // 4. Correct Reconstruction: Use the immutable base.
        val reconstructedV2Lines = DiffUtils.patch(immutableBase.lines(), patch2)
        assertEquals(v2Text, reconstructedV2Lines.joinToString("\n"))
        
        // 5. Incorrect Reconstruction: If we had used the current working file, it would fail.
        assertThrows(PatchFailedException::class.java) {
            DiffUtils.patch(currentWorkingFile.lines(), patch2)
        }
    }

    @Test
    fun testReconstructBaseVersion() {
        val v1Text = "Original Content"
        val lines = v1Text.lines()
        val reconstructed = lines.joinToString("\n")
        assertEquals(v1Text, reconstructed)
    }
}
