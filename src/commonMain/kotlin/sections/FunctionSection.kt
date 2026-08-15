package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.CompositeType
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer

data class FunctionSection(val types: List<UInt>) : Section, Printable {
    override val id: Byte
        get() = Constants.FUNCTION_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(types) { writeUInt(it) }
    }

    override fun Printer.print() {
        for(type in types) {
            names.defineFunction(type)
            names.currentFunction++
        }
    }

    constructor(reader: WasmReader) : this(reader.readList { it.readUInt() })

    companion object {
        val EMPTY = FunctionSection(listOf())
    }
}
