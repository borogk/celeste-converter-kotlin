package io.borogk.celeste

import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Path

class FilesConverter(private val graphicsConverter: GraphicsConverter) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun dataToPng(from: Path, to: Path) {
        logger.info("Converting DATA -> PNG")
        convert(
            from = from,
            to = to,
            fromExtension = ".data",
            toExtension = ".png",
            convert = graphicsConverter::dataToPng
        )
    }

    fun pngToData(from: Path, to: Path) {
        logger.info("Converting PNG -> DATA")
        convert(
            from = from,
            to = to,
            fromExtension = ".png",
            toExtension = ".data",
            convert = graphicsConverter::pngToData
        )
    }

    private fun convert(
        from: Path,
        to: Path,
        fromExtension: String,
        toExtension: String,
        convert: (InputStream, OutputStream) -> Unit
    ) {
        logger.info("From directory: {}", from)
        logger.info("To directory: {}", to)

        val subPaths = from.scanDirectory(fromExtension)
        logger.info("{} files to convert", subPaths.size)

        for ((i, subPath) in subPaths.withIndex()) {
            logger.info("[{}/{}] converting {}", i + 1, subPaths.size, subPath)

            val inputPath = from.resolve(subPath)
            val inputFileName = inputPath.fileName.toString()
            val outputFileName = inputFileName.substring(0, inputFileName.length - fromExtension.length) + toExtension
            val outputPath = to.resolve(subPath).resolveSibling(outputFileName)

            Files.createDirectories(outputPath.parent)
            inputPath.toInputStream().use { input ->
                outputPath.toOutputStream().use { output ->
                    convert(input, output)
                }
            }
        }
    }

    private fun Path.scanDirectory(extension: String, depth: Int = 32): List<Path> =
        Files.find(this, depth, { path, attributes ->
            path.toString().endsWith(extension, ignoreCase = true) && attributes.isRegularFile
        }).use { paths ->
            paths.map { path -> this.relativize(path) }.toList()
        }

    private fun Path.toInputStream(): InputStream = BufferedInputStream(FileInputStream(this.toFile()))

    private fun Path.toOutputStream(): OutputStream = BufferedOutputStream(FileOutputStream(this.toFile()))
}
