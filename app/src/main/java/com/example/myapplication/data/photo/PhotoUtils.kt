package com.example.myapplication.data.photo

object PhotoUtils {
    // EXIF orientation: clockwise rotation, then horizontal reflection.
    fun orientationTransform(orientation: Int): Pair<Int, Boolean> = when(orientation) {
        2 -> 0 to true
        3 -> 180 to false
        4 -> 180 to true
        5 -> 90 to true
        6 -> 90 to false
        7 -> 270 to true
        8 -> 270 to false
        else -> 0 to false
    }


    /**
     * How much to shrink a picture while decoding it (BitmapFactory wants a power of 2).
     * We halve the size as long as the longer side stays >= maxSize, so we never
     * decode a picture that is smaller than needed.
     * Example: 4000x3000, maxSize 1280 -> 2 (decoded as 2000x1500).
     */
    fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
        require(width > 0 && height > 0 && maxSize > 0)
        var sampleSize = 1
        var longerSide = maxOf(width, height)
        while (longerSide / 2 >= maxSize) {
            longerSide /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    /** Final size so that the longer side is at most maxSize (keeps the aspect ratio). */
    fun scaledSize(width: Int, height: Int, maxSize: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && maxSize > 0)
        val longerSide = maxOf(width, height)
        if (longerSide <= maxSize) return width to height
        val scale = maxSize.toFloat() / longerSide
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }
}
