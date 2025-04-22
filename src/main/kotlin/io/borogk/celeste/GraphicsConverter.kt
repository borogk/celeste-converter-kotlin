package io.borogk.celeste

import com.google.common.io.LittleEndianDataInputStream
import com.google.common.io.LittleEndianDataOutputStream
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import javax.imageio.ImageIO

class GraphicsConverter {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun dataToPng(input: InputStream, output: OutputStream) {
        LittleEndianDataInputStream(input).use { dataInputStream ->
            // Read image headers (width, height and alpha channel flag)
            val width = dataInputStream.readInt()
            val height = dataInputStream.readInt()
            val hasAlpha = dataInputStream.readBoolean()

            logger.info("DATA image parameters: {}x{}, {}", width, height, if (hasAlpha) "ARGB" else "RGB")

            val png = BufferedImage(width, height, if (hasAlpha) TYPE_INT_ARGB else TYPE_INT_RGB)

            var i = 0
            while (i < width * height) {
                // Read RLE count (must be between 1 and 255)
                val count = dataInputStream.read()
                if (count == 0) {
                    throw Exception("Unexpected count value of 0")
                }
                if (count == -1) {
                    throw EOFException("Unexpected end of stream before all pixels were read")
                }

                // Read individual channel values
                val a: Int
                val b: Int
                val g: Int
                val r: Int
                if (hasAlpha) {
                    a = dataInputStream.read()
                    if (a != 0) {
                        b = dataInputStream.read()
                        g = dataInputStream.read()
                        r = dataInputStream.read()
                    } else {
                        // Fully transparent pixels don't have color values
                        b = 0
                        g = 0
                        r = 0
                    }
                } else {
                    a = 0
                    b = dataInputStream.read()
                    g = dataInputStream.read()
                    r = dataInputStream.read()
                }

                // Merge channel values into a single (A)RGB value
                val rgb: Int = b and 0xFF or (g and 0xFF shl 8) or (r and 0xFF shl 16) or (a and 0xFF shl 24)

                // Output the next span of same-colored pixels
                for (j in 0 until count) {
                    val x = (i + j) % width
                    val y = (i + j) / width
                    png.setRGB(x, y, rgb)
                }

                i += count
            }

            if (!ImageIO.write(png, "png", output)) {
                throw Exception("Failed to write an image")
            }
        }
    }

    fun pngToData(input: InputStream, output: OutputStream) {
        val png = ImageIO.read(input)
        val hasAlpha = png.colorModel.hasAlpha()

        logger.info("PNG image parameters: {}x{}, {}", png.width, png.height, if (hasAlpha) "ARGB" else "RGB")

        LittleEndianDataOutputStream(output).use { dataOutputStream ->
            // Write image headers (width, height and alpha channel flag)
            dataOutputStream.writeInt(png.width)
            dataOutputStream.writeInt(png.height)
            dataOutputStream.writeBoolean(hasAlpha)

            var i = 0
            while (i < png.width * png.height) {
                // Take color value of the current pixel
                val x = i % png.width
                val y = i / png.width
                val rgb = png.getRGB(x, y)

                // Calculate RLE count by looking ahead at the next pixels
                var count = 1
                do {
                    // Don't step out of bounds
                    if (i + count >= png.width * png.height) {
                        break
                    }

                    // Compare with next pixel color
                    val x2 = (i + count) % png.width
                    val y2 = (i + count) / png.width
                    val rgb2 = png.getRGB(x2, y2)
                    if (rgb2 != rgb) {
                        break
                    }

                    // Increment, but don't exceed maximum 8-bit value
                    if (++count == 0xFF) {
                        break
                    }
                } while (true)

                // Extract individual channel values
                val b = rgb and 0xFF
                val g = rgb shr 8 and 0xFF
                val r = rgb shr 16 and 0xFF
                val a = if (hasAlpha) rgb shr 24 and 0xFF else 0

                // Write RLE count and (A)RGB channel values
                dataOutputStream.write(count)
                if (hasAlpha) {
                    dataOutputStream.write(a)
                    if (a != 0) {
                        dataOutputStream.write(b)
                        dataOutputStream.write(g)
                        dataOutputStream.write(r)
                    }
                } else {
                    dataOutputStream.write(b)
                    dataOutputStream.write(g)
                    dataOutputStream.write(r)
                }

                i += count
            }
        }
    }
}
