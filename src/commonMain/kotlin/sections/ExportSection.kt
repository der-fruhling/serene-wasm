package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.Export
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

data class ExportSection(val exports: List<Export>) : Section, Printable {
    override val id: Byte
        get() = Constants.EXPORT_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(exports)
    }

    override fun Printer.print() {
        for(export in exports) wrap {
            word("export")
            export.print(this)
        }
    }

    constructor(reader: WasmReader) : this(reader.readList(::Export))

    companion object {
        val EMPTY = ExportSection(emptyList())
    }
}
