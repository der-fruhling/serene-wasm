package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.MemoryType
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

data class MemorySection(val memoryTypes: List<MemoryType>) : Section, Printable {
    override val id: Byte
        get() = Constants.MEMORY_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(memoryTypes)
    }

    override fun Printer.print() {
        for(type in memoryTypes) wrap {
            word("memory")
            word(names.resolveNameDecl(Namespace.MEMORY, names.currentMemory))
            type.print(this)
            names.currentMemory++
        }
    }

    constructor(reader: WasmReader) : this(reader.readList { MemoryType.decode(it) })

    companion object {
        val EMPTY = MemorySection(emptyList())
    }
}