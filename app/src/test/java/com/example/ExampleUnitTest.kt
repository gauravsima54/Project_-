package com.example

import android.graphics.Bitmap
import com.example.camera.film.FilmEngine
import com.example.camera.model.CameraMode
import com.example.camera.model.FilmProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

  @Test
  fun testAllFilmProfilesGenerateValidColorMatrix() {
    assertEquals(8, FilmProfile.entries.size)
    for (profile in FilmProfile.entries) {
      val matrix = profile.buildColorMatrix()
      assertEquals("Profile ${profile.name} matrix must have 20 elements", 20, matrix.size)
    }
  }

  @Test
  fun testFilmEngineBitmapProcessing() {
    val src = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val result = FilmEngine.processBitmap(
      source = src,
      profile = FilmProfile.CINEMATIC,
      evOffset = 0.5f,
      contrastDelta = 0.1f,
      saturationDelta = 0.2f
    )
    assertNotNull(result)
    assertEquals(100, result.width)
    assertEquals(100, result.height)
  }

  @Test
  fun testCameraModes() {
    assertEquals(5, CameraMode.entries.size)
    assertNotNull(CameraMode.valueOf("PHOTO"))
    assertNotNull(CameraMode.valueOf("VIDEO"))
    assertNotNull(CameraMode.valueOf("PORTRAIT"))
    assertNotNull(CameraMode.valueOf("CINEMATIC"))
    assertNotNull(CameraMode.valueOf("PRO"))
  }
}
