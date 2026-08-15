package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.DataSegment
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

data class DataSection(val data: List<DataSegment>) : Section, Printable {
    override val id: Byte
        get() = Constants.DATA_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(data)
    }

    override fun Printer.print() {
        for(segment in data) wrap {
            word("data")
            word(names.resolveNameDecl(Namespace.DATA, names.currentData))
            segment.print(this)
            names.currentData++
        }
    }

    constructor(reader: WasmReader) : this(reader.readList { DataSegment.decode(it) })

    companion object {
        val EMPTY = DataSection(emptyList())
    }
}
