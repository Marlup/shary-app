package com.shary.app.ui.screens.field

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldScreenLayoutPolicyTest {

    @Test
    fun buildVaultNudgeShowsOnlyForAnUnfilteredSparseVault() {
        assertTrue(shouldShowBuildVaultNudge(totalFieldCount = 1, query = "", activeFilter = "All"))
        assertTrue(shouldShowBuildVaultNudge(totalFieldCount = 2, query = "", activeFilter = "All"))
        assertFalse(shouldShowBuildVaultNudge(totalFieldCount = 3, query = "", activeFilter = "All"))
        assertFalse(shouldShowBuildVaultNudge(totalFieldCount = 1, query = "passport", activeFilter = "All"))
        assertFalse(shouldShowBuildVaultNudge(totalFieldCount = 1, query = "", activeFilter = "Identity"))
    }
}
