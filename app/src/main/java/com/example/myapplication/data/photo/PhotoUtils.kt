package com.example.myapplication.data.photo

object PhotoUtils {

    /**
     * How much to shrink a picture while decoding it (BitmapFactory wants a power of 2).
     * We halve the size as long as the longer side stays >= maxSize, so we never
     * decode a picture that is smaller than needed.
     * Example: 4000x3000, maxSize 1280 -> 2 (decoded as 2000x1500).
     */
    fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
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
        val longerSide = maxOf(width, height)
        if (longerSide <= maxSize) return width to height
        val scale = maxSize.toFloat() / longerSide
        return (width * scale).toInt() to (height * scale).toInt()
    }
}
