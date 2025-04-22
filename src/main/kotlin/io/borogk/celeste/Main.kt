package io.borogk.celeste

import java.nio.file.Path

private val graphicsConverter = GraphicsConverter()
private val filesConverter = FilesConverter(graphicsConverter)

fun main(args: Array<String>) {
    if (args.size < 3) {
        throw Exception("Too few arguments")
    }

    val command = args[0]
    val from = Path.of(args[1])
    val to = Path.of(args[2])

    when (command) {
        "data2png" -> filesConverter.dataToPng(from, to)
        "png2data" -> filesConverter.pngToData(from, to)
        else -> throw Exception("Unrecognised command $command")
    }
}
