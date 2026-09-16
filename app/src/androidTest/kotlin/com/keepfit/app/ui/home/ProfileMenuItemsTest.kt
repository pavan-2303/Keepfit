package com.keepfit.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.keepfit.core.model.BodyProfile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileMenuItemsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun multipleProfilesExposeSwitchAddEditArchiveAndSettingsActions() {
        val alex = profile("alex", "Alex")
        val sam = profile("sam", "Sam")
        var selectedId: String? = null
        composeRule.setContent {
            Column {
                ProfileMenuItems(
                    profile = alex,
                    profiles = listOf(alex, sam),
                    onSelect = { selectedId = it },
                    onAdd = {},
                    onEdit = {},
                    onArchive = {},
                    onSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Alex (active)").assertIsDisplayed()
        composeRule.onNodeWithText("Sam").performClick()
        composeRule.onNodeWithText("Add profile").assertIsDisplayed()
        composeRule.onNodeWithText("Edit Alex").assertIsDisplayed()
        composeRule.onNodeWithText("Archive Alex").assertIsDisplayed()
        composeRule.onNodeWithText("Profile and settings").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals("sam", selectedId) }
    }

    @Test
    fun finalProfileCannotBeArchivedFromTheMenu() {
        val alex = profile("alex", "Alex")
        composeRule.setContent {
            Column {
                ProfileMenuItems(alex, listOf(alex), {}, {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText("Archive Alex").assertDoesNotExist()
    }

    private fun profile(id: String, name: String) = BodyProfile(
        id = id,
        displayName = name,
        heightCm = 170.0,
        birthDate = null,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
