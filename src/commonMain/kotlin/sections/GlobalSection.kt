package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.Global
import net.derfruhling.serene.wasm.module.GlobalType
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

data class GlobalSection(val globals: List<Global>) : Section, Printable {
    override val id: Byte
        get() = Constants.GLOBAL_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(globals)
    }

    override fun Printer.print() {
        for(global in globals) wrap {
            word("global")
            word(names.resolveNameDecl(Namespace.GLOBAL, names.currentGlobal))
            global.print(this)
            names.currentGlobal++
        }
    }

    constructor(reader: WasmReader) : this(reader.readList(::Global))

    companion object {
        val EMPTY = GlobalSection(emptyList())
    }
}
