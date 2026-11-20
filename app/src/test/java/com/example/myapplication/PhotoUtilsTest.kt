package com.example.myapplication

import com.example.myapplication.data.photo.PhotoUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoUtilsTest {

    @Test
    fun veryNarrowPhoto_stillHasPositiveDimensions() {
        assertEquals(1 to 1280, PhotoUtils.scaledSize(1, 10000, 1280))
        assertEquals(1280 to 1, PhotoUtils.scaledSize(10000, 1, 1280))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroTargetInsteadOfLooping() {
        PhotoUtils.calculateInSampleSize(4000, 3000, 0)
    }

    @Test
    fun handlesMirroredExifOrientations() {
        assertEquals(90 to true, PhotoUtils.orientationTransform(5))
        assertEquals(270 to true, PhotoUtils.orientationTransform(7))
        assertEquals(0 to true, PhotoUtils.orientationTransform(2))
        assertEquals(90 to false, PhotoUtils.orientationTransform(6))
        assertEquals(0 to false, PhotoUtils.orientationTransform(1))
    }

    @Test
    fun bigCameraPhoto_isDecodedSmaller() {
        assertEquals(2, PhotoUtils.calculateInSampleSize(4000, 3000, 1280))
        assertEquals(4, PhotoUtils.calculateInSampleSize(8000, 6000, 1280))
    }

    @Test
    fun smallPhoto_isNotScaled() {
        assertEquals(1, PhotoUtils.calculateInSampleSize(800, 600, 1280))
        assertEquals(800 to 600, PhotoUtils.scaledSize(800, 600, 1280))
    }

    @Test
    fun scaledSize_keepsAspectRatio() {
        assertEquals(1280 to 960, PhotoUtils.scaledSize(2000, 1500, 1280))
        assertEquals(960 to 1280, PhotoUtils.scaledSize(1500, 2000, 1280))
    }
}
