package io.borogk.celeste

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class GraphicsConverterTest {
    companion object {
        @JvmStatic
        fun allTestImages() = listOf(
            "white",
            "red",
            "green",
            "blue",
            "cyan",
            "magenta",
            "yellow",
            "black",
            "transparent",
            "multi-color",
            "big-test",
            "big-test-no-background",
        )
    }

    private val graphicsConverter = GraphicsConverter()

    @ParameterizedTest
    @MethodSource("allTestImages")
    fun `converted PNG corresponds to original`(image: String) {
        val originalImage = resourceToBytes("png/$image.png").toImage()
        val convertedImage = resourceToBytes("data/$image.data").toPngBytes().toImage()

        assertImageEquals(originalImage, convertedImage)
    }

    @ParameterizedTest
    @MethodSource("allTestImages")
    fun `image survives multiple conversions`(image: String) {
        val pngCopyBytes = resourceToBytes("data/$image.data").toPngBytes()
        val dataCopyBytes = pngCopyBytes.toDataBytes()
        val pngSecondCopyBytes = dataCopyBytes.toPngBytes()

        val convertedImage = pngCopyBytes.toImage()
        val twiceConvertedImage = pngSecondCopyBytes.toImage()

        assertImageEquals(convertedImage, twiceConvertedImage)
    }

    private fun resourceToBytes(resource: String): ByteArray =
        this.javaClass.classLoader.getResourceAsStream(resource).use { it!!.readAllBytes() }

    private fun ByteArray.toPngBytes(): ByteArray =
        ByteArrayInputStream(this).use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                graphicsConverter.dataToPng(inputStream, outputStream)
                outputStream.toByteArray()
            }
        }

    private fun ByteArray.toDataBytes(): ByteArray =
        ByteArrayInputStream(this).use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                graphicsConverter.pngToData(inputStream, outputStream)
                outputStream.toByteArray()
            }
        }

    private fun ByteArray.toImage(): BufferedImage =
        ImageIO.read(ByteArrayInputStream(this))

    private fun assertImageEquals(expected: BufferedImage, actual: BufferedImage) {
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)

        assertEquals(expected.colorModel.numComponents, actual.colorModel.numComponents)
        assertEquals(expected.colorModel.hasAlpha(), actual.colorModel.hasAlpha())

        for (x in 0 until expected.width) {
            for (y in 0 until expected.height) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y))
            }
        }
    }
}
