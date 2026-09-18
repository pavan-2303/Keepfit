package com.keepfit.app.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.keepfit.core.designsystem.KeepfitTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfileSetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setupMovesOneStepAtATimeAndCanSkipOptionalBaseline() {
        var savedName = ""
        var savedHeight = "not-called"
        var savedBirthDate: LocalDate? = LocalDate.MIN
        var savedWeight = "not-called"
        composeRule.setContent {
            KeepfitTheme {
                ProfileSetupScreen(
                    validationMessage = null,
                    onSave = { name, height, birthDate, weight ->
                        savedName = name
                        savedHeight = height
                        savedBirthDate = birthDate
                        savedWeight = weight
                    },
                )
            }
        }

        composeRule.onNodeWithText("Let’s start with you").assertIsDisplayed()
        composeRule.onNodeWithText("Name").performTextInput("Pavan")
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Your baseline").assertIsDisplayed()
        composeRule.onNodeWithText("Skip optional details").performClick()

        composeRule.runOnIdle {
            assertEquals("Pavan", savedName)
            assertEquals("", savedHeight)
            assertNull(savedBirthDate)
            assertEquals("", savedWeight)
        }
    }

    @Test
    fun baselineShowsLocalBmiWhenHeightAndWeightAreEntered() {
        composeRule.setContent {
            KeepfitTheme { ProfileSetupScreen(validationMessage = null, onSave = { _, _, _, _ -> }) }
        }

        composeRule.onNodeWithText("Name").performTextInput("Pavan")
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Height in cm").performTextInput("178")
        composeRule.onNodeWithText("Starting weight in kg").performTextInput("79.2")

        composeRule.onNodeWithText("BMI 25.0").assertIsDisplayed()
    }

    @Test
    fun optionalBaselineRemainsReachableAtCompactWidthAndLargeText() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                KeepfitTheme {
                    Box(Modifier.width(360.dp).height(640.dp)) {
                        ProfileSetupScreen(validationMessage = null, onSave = { _, _, _, _ -> })
                    }
                }
            }
        }

        composeRule.onNodeWithText("Name").performTextInput("Pavan")
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()
        composeRule.onNodeWithText("Your baseline").assertIsDisplayed()
        composeRule.onNodeWithText("Continue to assessment").performScrollTo().assertIsDisplayed()
    }
}
