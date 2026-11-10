package com.spendwise.app.export

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMergeTest {
    @Test
    fun mergePreservesMultiplicityAndCanBeRepeated() {
        val current = listOf("November expense", "July expense")
        val backup = listOf("July expense", "July expense", "October expense")
        val added = missingRows(current, backup) { it }

        assertEquals(listOf("July expense", "October expense"), added)
        assertEquals(emptyList<String>(), missingRows(current + added, backup) { it })
    }
}
