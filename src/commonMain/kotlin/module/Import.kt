package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.Encode
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print
import net.derfruhling.serene.wasm.printer.string

data class Import(val module: String, val name: String, val externType: ExternType) : Encode {
    override fun encode(out: WasmWriter) {
        out.writeString(module)
        out.writeString(name)
        externType.encode(out)
    }

    fun Printer.print(itemName: String) {
        string(module)
        string(name)
        wrapInline { externType.print(this, itemName) }
    }

    constructor(reader: WasmReader) : this(
        reader.readString(),
        reader.readString(),
        ExternType.decode(reader)
    )
}

fun Import.print(printer: Printer, itemName: String) {
    with(printer) { print(itemName) }
}
