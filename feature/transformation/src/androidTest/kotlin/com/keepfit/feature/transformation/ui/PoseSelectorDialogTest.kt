package com.keepfit.feature.transformation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.keepfit.core.model.TransformationPose
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PoseSelectorDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsFourFixedBasicsAndAllowsAnOptionalPoseToBeEnabled() {
        var toggled: Pair<TransformationPose, Boolean>? = null
        composeRule.setContent {
            MaterialTheme {
                PoseSelectorDialog(
                    enabledPoses = TransformationPose.defaultPoses,
                    onToggle = { pose, enabled -> toggled = pose to enabled },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("• Front relaxed").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Enable Front - hands on hips")
            .assertIsOff()
            .performClick()

        assertEquals(TransformationPose.FRONT_HANDS_ON_HIPS to true, toggled)
    }

    @Test
    fun reflectsPreviouslyEnabledOptionalPose() {
        composeRule.setContent {
            MaterialTheme {
                PoseSelectorDialog(
                    enabledPoses = TransformationPose.defaultPoses + TransformationPose.FRONT_HANDS_ON_HIPS,
                    onToggle = { _, _ -> },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Enable Front - hands on hips").assertIsOn()
    }
}
