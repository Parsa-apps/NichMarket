package com.parsaapps.nichmarket

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parsaapps.nichmarket.ui.screens.about.PublisherIdentity
import com.parsaapps.nichmarket.ui.theme.NichMarketTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PublisherIdentityUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun identityIsAccessibleWhenMotionIsReduced() {
        compose.setContent { NichMarketTheme { PublisherIdentity(reduceMotion = true) } }
        compose.onNodeWithContentDescription("لوگوی رسمی پارسا اپس").assertIsDisplayed()
        compose.onNodeWithText("فرشاد پارسا • مدیریت و توسعه اصلی").assertIsDisplayed()
    }

    @Test fun identityIsAccessibleWithMotionEnabled() {
        compose.setContent { NichMarketTheme { PublisherIdentity(reduceMotion = false) } }
        compose.onNodeWithText("پارسا اپس").assertIsDisplayed()
        compose.onNodeWithContentDescription("لوگوی رسمی پارسا اپس").assertIsDisplayed()
    }
}
