package com.keepfit.feature.settings.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ExerciseCatalogueNoticeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bundledCatalogueNoticeIsCompactUntilTheUserRequestsDetails() {
        composeRule.setContent {
            MaterialTheme { ExerciseCatalogueLegalNotice() }
        }

        composeRule.onNodeWithText("Open-source licences").assertIsDisplayed()
        composeRule.onNodeWithText("Third-party catalogue and library notices.").assertIsDisplayed()
        composeRule.onNodeWithText("Exercises Dataset by Hasan Emir Yıldırım", substring = true).assertDoesNotExist()

        composeRule.onNodeWithText("View details").performClick()

        composeRule.onNodeWithText("Exercises Dataset by Hasan Emir Yıldırım", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("MIT License", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("7455efae41b3", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Gym visual images and GIFs are not included", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("25 original Keepfit movement figures", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("code-native artwork", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Hide details").assertIsDisplayed()
    }
}
