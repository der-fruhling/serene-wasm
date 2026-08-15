package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer

data class StartSection(val startFunctionIdx: UInt) : Section, Printable {
    override val id: Byte
        get() = Constants.START_SECTION

    override fun encode(out: WasmWriter) {
        out.writeUInt(startFunctionIdx)
    }

    override fun Printer.print() = wrap {
        word("start")
        word(names.resolveName(Namespace.FUNC, startFunctionIdx))
    }

    constructor(reader: WasmReader) : this(reader.readUInt())
}
