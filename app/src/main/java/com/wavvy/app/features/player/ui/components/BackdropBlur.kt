package com.wavvy.app.features.player.ui.components

// Android graphics
import android.graphics.Bitmap
// Image loading
import coil3.size.Size
import coil3.transform.Transformation

// Blurs a tiny copy of the cover once, so the backdrop of the open player is a plain image that costs nothing to draw on each frame
class BackdropBlur(
    private val radius: Int,
    private val passes: Int
) : Transformation() {
    override val cacheKey: String = "backdrop_blur_${radius}_$passes"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Hardware bitmaps cannot be read, a software copy can
        val source = if (input.config == Bitmap.Config.HARDWARE) input.copy(Bitmap.Config.ARGB_8888, false) else input
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // A few box blurs in a row look like a gaussian blur
        val buffer = IntArray(pixels.size)
        repeat(passes) {
            boxBlur(pixels, buffer, width, height, horizontal = true)
            boxBlur(buffer, pixels, width, height, horizontal = false)
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    // Average of the pixels around each pixel along one direction, the edges repeat the border pixel
    private fun boxBlur(from: IntArray, to: IntArray, width: Int, height: Int, horizontal: Boolean) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val window = radius * 2 + 1

        for (line in 0 until lines) {
            var alpha = 0
            var red = 0
            var green = 0
            var blue = 0

            // Pixel at a place of the line, clamped to its ends
            fun at(index: Int): Int {
                val clamped = index.coerceIn(0, length - 1)
                return if (horizontal) from[line * width + clamped] else from[clamped * width + line]
            }

            for (index in -radius..radius) {
                val pixel = at(index)
                alpha += pixel ushr 24
                red += (pixel shr 16) and 0xFF
                green += (pixel shr 8) and 0xFF
                blue += pixel and 0xFF
            }

            for (index in 0 until length) {
                val target = if (horizontal) line * width + index else index * width + line
                to[target] = ((alpha / window) shl 24) or ((red / window) shl 16) or ((green / window) shl 8) or (blue / window)

                // Slides the window one pixel forward
                val leaving = at(index - radius)
                val entering = at(index + radius + 1)
                alpha += (entering ushr 24) - (leaving ushr 24)
                red += ((entering shr 16) and 0xFF) - ((leaving shr 16) and 0xFF)
                green += ((entering shr 8) and 0xFF) - ((leaving shr 8) and 0xFF)
                blue += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }
}
