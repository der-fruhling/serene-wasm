package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.RecursiveType
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.identWord
import net.derfruhling.serene.wasm.printer.print

data class TypeSection(val types: List<RecursiveType>) : Section, Printable {
    override val id: Byte
        get() = Constants.TYPE_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(types)
    }

    constructor(reader: WasmReader) : this(reader.readList { RecursiveType.decode(it) })

    override fun Printer.print() {
        for (type in types) wrap {
            when(type) {
                is RecursiveType.Compound -> type.print(this)
                else -> {
                    word("type")
                    word(names.resolveNameDecl(Namespace.TYPE, names.currentType))
                    wrapInline { type.print(this) }
                    names.currentType++
                }
            }
        }
    }

    companion object {
        val EMPTY = TypeSection(emptyList())
    }
}
