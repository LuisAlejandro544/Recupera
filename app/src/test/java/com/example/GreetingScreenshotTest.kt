package com.example

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.RecoverablePhoto
import com.example.model.RecoverySource
import com.example.ui.components.PhotoCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun photoRecoveryCardScreenshot() {
    val photo = RecoverablePhoto(
        id = "test_1",
        name = "IMG_Recovered.jpg",
        filePath = "/storage/emulated/0/DCIM/.thumbnails/thumb.jpg",
        fileSizeBytes = 1_048_576L,
        lastModifiedTimestamp = 1700000000000L,
        sourceCategory = RecoverySource.TRASH_MEDIASTORE,
        fileExtension = "jpg",
        dimensions = "1920x1080"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        Surface(color = DarkBackground) {
          PhotoCard(
              photo = photo,
              onPhotoClick = {},
              onToggleSelect = {}
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/photo_card.png")
  }
}
