package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.CompositeType
import net.derfruhling.serene.wasm.module.ExternType
import net.derfruhling.serene.wasm.module.Import
import net.derfruhling.serene.wasm.module.print
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer

data class ImportSection(val imports: List<Import>) : Section, Printable {
    override val id: Byte
        get() = Constants.IMPORT_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(imports)
    }

    override fun Printer.print() {
        for(import in imports) wrap {
            word("import")
            val (ns, index) = when(import.externType) {
                is ExternType.Func -> Namespace.FUNC to names.currentFunction
                is ExternType.Global -> Namespace.GLOBAL to names.currentGlobal
                is ExternType.Memory -> Namespace.MEMORY to names.currentMemory
                is ExternType.Table -> Namespace.TABLE to names.currentTable
                is ExternType.Tag -> Namespace.TAG to names.currentTag
            }
            import.print(this, names.resolveNameDecl(ns, index))
            when(ns) {
                Namespace.FUNC -> {
                    names.defineFunction(index)
                    names.currentFunction++
                }
                Namespace.GLOBAL -> names.currentGlobal++
                Namespace.MEMORY -> names.currentMemory++
                Namespace.TABLE -> names.currentTable++
                Namespace.TAG -> names.currentTag++
                else -> error("unreachable")
            }
        }
    }

    constructor(reader: WasmReader) : this(reader.readList(::Import))

    companion object {
        val EMPTY = ImportSection(emptyList())
    }
}
