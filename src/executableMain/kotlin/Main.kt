package net.derfruhling.serene.wasm

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.oshai.kotlinlogging.DirectLoggerFactory
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import net.derfruhling.serene.wasm.printer.SimplePrinterVisitor
import net.derfruhling.serene.wasm.printer.printer

private data class CommandConfig(
    var verbose: Boolean = false
)

private class SereneWasmCommand : CoreCliktCommand() {
    val verbose by option().flag("--no-verbose")
    val config by findOrSetObject { CommandConfig() }

    override fun run() {
        config.verbose = verbose

        if(KotlinLoggingConfiguration.loggerFactory == DirectLoggerFactory) {
            KotlinLoggingConfiguration.direct.logLevel = if(verbose) Level.DEBUG else Level.INFO
        }
    }
}

private class PrintCommand : CoreCliktCommand() {
    val config by requireObject<CommandConfig>()
    val file by argument()
    val output by option("-o", "--output")

    override fun run() {
        val output = output
        if(output != null) {
            val string = buildString { run(this) }
            SystemFileSystem.sink(Path(output)).buffered().use { it.writeString(string) }
        } else {
            val appendable: Appendable = object : Appendable {
                override fun append(value: Char): Appendable = also { print(value) }
                override fun append(value: CharSequence?): Appendable = also { print(value) }

                override fun append(
                    value: CharSequence?,
                    startIndex: Int,
                    endIndex: Int
                ): Appendable = append(value?.subSequence(startIndex, endIndex))
            }

            run(appendable)
        }
    }

    private fun run(appendable: Appendable) {
        SystemFileSystem.source(Path(file)).buffered().use { source ->
            val reader = WasmReader(source)
            val parser = WasmParser(reader, SimplePrinterVisitor(null, appendable.printer()))
            parser.parseModule()
        }
    }
}

fun main(args: Array<String>) = SereneWasmCommand()
    .subcommands(PrintCommand())
    .main(args)
